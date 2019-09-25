///  helper.ts
///  Evaluation helper app.
///


//  Utility functions.
//
function toArray(coll: HTMLCollectionOf<Element>): Element[] {
    let a = [] as Element[];
    for (let i = 0; i < coll.length; i++) {
        a.push(coll[i]);
    }
    return a;
}

function makeSelect(e: Element, id: string, choices: string[]) {
    let select = document.createElement('select');
    select.id = id;
    for (let k of choices) {
        let i = k.indexOf(':');
        let option = document.createElement('option');
        let v = k.substr(0, i);
        option.setAttribute('value', v);
        option.innerText = v+') '+k.substr(i+1);
        select.appendChild(option);
    }
    e.appendChild(select);
    return select;
}

function makeInput(e: Element, id: string, size: number) {
    let input = document.createElement('input');
    input.id = id;
    input.size = size;
    e.appendChild(input);
    return input;
}

function makeCheckbox(e: Element, id: string, caption: string) {
    let label = document.createElement('label');
    let checkbox = document.createElement('input');
    checkbox.id = id;
    checkbox.setAttribute('type', 'checkbox');
    label.appendChild(checkbox);
    label.appendChild(document.createTextNode(' '+caption));
    e.appendChild(label);
    return checkbox;
}


//  Item
//
class Item {

    cid: string;
    choice: string = '';
    updated: number = 0;
    comment: string = '';

    constructor(cid: string) {
	this.cid = cid;
    }

    setChoice(choice: string) {
	this.choice = choice;
        this.updated = Date.now();
    }

    setComment(comment: string) {
	this.comment = comment;
        this.updated = Date.now();
    }

    load(obj: any) {
        this.choice = obj['choice']
        this.updated = obj['updated']
	this.comment = obj['comment']
    }

    save() {
	let obj = {
	    'cid': this.cid,
	    'choice': this.choice,
	    'updated': this.updated,
	    'comment': this.comment,
	}
	return obj;
    }
}


//  ItemList
//
interface ItemList {
    [index: string]: Item;
}


//  ItemDB
//
class ItemDB {

    textarea: HTMLTextAreaElement;
    fieldName: string;
    items: ItemList;

    constructor(textarea: HTMLTextAreaElement, fieldName: string) {
	this.textarea = textarea;
	this.fieldName = fieldName;
	this.items = {};
    }

    init(choices: string[]) {
	for (let e of toArray(document.getElementsByClassName('ui'))) {
            let cid = e.id;
	    e.appendChild(document.createTextNode('Evaluation: '));
            let sel1 = makeSelect(e, 'SC'+cid, choices);
            sel1.addEventListener(
		'change', (ev) => { this.onChoiceChanged(ev); });
	    e.appendChild(document.createTextNode(' \xa0 Comment: '));
            let fld1 = makeInput(e, 'IC'+cid, 30);
            fld1.addEventListener(
		'change', (ev) => { this.onCommentChanged(ev); });
            e.appendChild(document.createTextNode(' '));
            this.items[cid] = new Item(cid);
	}
	this.loadTextArea();
	this.textarea.addEventListener(
	    'input', (ev) => { this.onTextChanged(ev); });
	this.importData();
	this.updateHTML();
	console.info("init");
    }

    onTextChanged(ev: Event) {
        this.importData();
        this.updateHTML();
	this.saveTextArea();
	console.info("onTextChanged");
    }

    onChoiceChanged(ev: Event) {
        let e = ev.target as HTMLSelectElement;
        let cid = e.id.substr(2);
        let item = this.items[cid];
	item.setChoice(e.value);
        this.exportData();
	this.saveTextArea();
	console.info("onChoiceChanged: ", cid);
    }

    onCommentChanged(ev: Event) {
        let e = ev.target as HTMLInputElement;
        let cid = e.id.substr(2);
        let item = this.items[cid];
	item.setComment(e.value);
        this.exportData();
	this.saveTextArea();
	console.info("onCommentChanged: ", cid);
    }

    saveTextArea() {
	if (window.localStorage) {
            window.localStorage.setItem(this.fieldName, this.textarea.value);
	}
    }

    loadTextArea() {
	if (window.localStorage) {
            this.textarea.value = window.localStorage.getItem(this.fieldName);
	}
    }

    importData() {
	let text = this.textarea.value;
	for (let line of text.split(/\n/)) {
            line = line.trim();
            if (line.length == 0) continue;
            if (line.substr(0,1) == '#') continue;
	    let obj = JSON.parse(line);
            let item = new Item(obj['cid']);
            item.load(obj);
            this.items[item.cid] = item;
	}
    }

    exportData() {
	let cids = Object.getOwnPropertyNames(this.items);
	let lines = [] as string[];
	lines.push('#START '+this.fieldName);
	for (let cid of cids.sort()) {
            let item = this.items[cid];
	    let obj = item.save();
            lines.push(JSON.stringify(obj));
	}
	lines.push('#END');
	this.textarea.value = lines.join('\n');
    }

    updateHTML() {
	let cids = Object.getOwnPropertyNames(this.items);
	for (let cid of cids) {
            let item = this.items[cid];
            let sel = document.getElementById('SC'+cid) as HTMLSelectElement;
            if (sel !== null) {
		sel.value = item.choice;
            }
            let fld = document.getElementById('IC'+cid) as HTMLInputElement;
	    if (fld !== null) {
		fld.value = item.comment;
	    }
	}
    }
}

// main
function run(id: string, fieldName: string, choices: string[]) {
    let textarea = document.getElementById(id) as HTMLTextAreaElement;
    let db = new ItemDB(textarea, fieldName);
    db.init(choices);
}
