package complete;

import java.io.IOException;

public interface Mth {

    void error(int n) throws IOException;
    void getsym() throws IOException;
    void getch() throws IOException;
    void init() throws IOException;
    void gen(Main.fct x, int y, int z);
    void test(boolean[] s1, boolean[] s2, int n) throws IOException;
    boolean inset(int e, boolean[] s) throws IOException;
    int addset(boolean sr[], boolean s1[], boolean s2[], int n) throws IOException;
    int subset(boolean sr[], boolean s1[], boolean s2[], int n) throws IOException;
    int mulset(boolean sr[], boolean s1[], boolean s2[], int n) throws IOException;
    void block(int lev, int tx, boolean fsys[]) throws IOException;
    void interpret();
    void factor(boolean[] fsys, Integer ptx, int lev) throws IOException;
    void term(boolean[] fsys, Integer ptx, int lev) throws IOException;
    void condition(boolean[] fsys, Integer ptx, int lev) throws IOException;
    void expression(boolean[] fsys, Integer ptx, int lev) throws IOException;
    void statement(boolean[] fsys, Integer ptx, int lev) throws IOException;
    void listcode(int cx0);
    void listall();
    void vardeclaration(Integer ptx, int lev, Integer pdx) throws IOException;
    void constdeclaration(Integer ptx, int lev, Integer pdx) throws IOException;
    int position(String idt, int tx) throws IOException;
    void enter(Main.object k, Integer ptx, int lev, Integer pdx) throws IOException;
    int base(int l, Integer s, int b);
}
