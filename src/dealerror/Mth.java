package dealerror;

import java.io.IOException;

public interface Mth {

    void error(int n) throws IOException;
    void getsym() throws IOException;
    void getch() throws IOException;
    void init() throws IOException;
    void test(boolean[] s1, boolean[] s2, int n) throws IOException;
    boolean inset(int e, boolean[] s) throws IOException;
    int addset(boolean sr[], boolean s1[], boolean s2[], int n) throws IOException;
    int subset(boolean sr[], boolean s1[], boolean s2[], int n) throws IOException;
    int mulset(boolean sr[], boolean s1[], boolean s2[], int n) throws IOException;
    void block(int tx, boolean fsys[]) throws IOException;
    void factor(boolean[] fsys, Integer ptx) throws IOException;
    void term(boolean[] fsys, Integer ptx) throws IOException;
    void condition(boolean[] fsys, Integer ptx) throws IOException;
    void expression(boolean[] fsys, Integer ptx) throws IOException;
    void statement(boolean[] fsys, Integer ptx) throws IOException;
    void vardeclaration(Integer ptx) throws IOException;
    void constdeclaration(Integer ptx) throws IOException;
    int position(String idt, int tx) throws IOException;
    void enter(Main.object k, Integer ptx) throws IOException;
}
