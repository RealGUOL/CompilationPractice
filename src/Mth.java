import java.io.IOException;

public interface Mth {
    void error(int n);
    void getsym() throws IOException;
    void getch() throws IOException;
    void init();
    void block(Integer tx) throws IOException;
    void factor(Integer ptx) throws IOException;
    void term(Integer ptx)throws IOException;
    void condition(Integer ptx)throws IOException;
    void expression(Integer ptx) throws IOException;
    void statement(Integer ptx) throws IOException;
    void vardeclaration(Integer ptx) throws IOException;
    void constdeclaration(Integer ptx) throws IOException;
    int posision(String idt, Integer tx);
    void enter(Main.object k, Integer ptx);
}
