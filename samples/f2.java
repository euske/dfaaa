public class Foo {
    
    public static void f2(int x, int y) {
	int z = x+1;
	if (z < y) {
	    int a = z;
	    x += a;
	} else {
	    z = 2;
	}
	if (x == 0) {
	    return y++;
	} else {
	    return x*z;
	}
    }
    
}
