package dom.meep;

public class multi_xref1 {

    multi_xref2 xref2;

    class baa {
        int baz;
    }

    public void moo() {
	this.xref2 = new multi_xref2();
	xref2.foo();
        multi_xref2.bam();
        dom.dood.multi_xref3.boo = 123;
    }
}
