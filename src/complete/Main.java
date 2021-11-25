package complete;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static complete.Main.symbol.*;

public class Main implements Mth {

    int norw = 13;    // 保留字个数
    int txmax = 100;  // 符号表容量
    int nmax = 14;    // 数字的最大位数
    int al = 10;      // 标识符的最大长度
    int maxerr = 30;  // 允许的最多错误数
    int amax = 2048;  // 地址上界
    int levmax = 3;   // 最大允许过程嵌套声明层数
    int cxmax = 200;  // 最多的虚拟即代码数
    int stacksize = 500; // 运行时数据栈元素最多为500个

    // 符号
    enum symbol {
        nul, ident, number, plus, minus,
        times, slash, oddsym, eql, neq,
        lss, leq, gtr, geq, lparen,
        rparen, comma, semicolon, period, becomes,
        beginsym, endsym, ifsym, thensym, whilesym,
        writesym, readsym, dosym, callsym, constsym,
        varsym, procsym,
    }

    int sysnum = 32;

    // 符号表中的类型
    enum object {
        constant,
        variable,
        procedure,
    }

    // 虚拟机代码指令
    enum fct {
        lit, opr, lod,
        sto, cal, ini,
        jmp, jpc,
    }
    int fctnum = 8;

    // 虚拟机代码结构
    class instruction {
        fct f;  // 虚拟机代码指令
        int l;  // 引用层和声明层的层次差
        int a;  // 根据f的不同而不同
    }

    boolean listswitch;  // 显示虚拟机代码与否
    boolean tableswitch; // 显示符号表与否
    char ch;    // 存放当前读取的字符，getch使用
    symbol sym;  // 当前的符号
    String id; // 当前ident，多出的一个字符用于存放0
    int num;    // 当前的number
    int cc, ll;  // getch 使用的计数器，cc表示当前字符(ch)的位置
    int cx;    // 虚拟机代码指针，取值范围[0, cxnax-1]
    char line[] = new char[81]; // 读取行缓冲区
    String a;  // 临时符号，多出的一个字节用于存放0
    instruction code[] = new instruction[cxmax]; // 存放虚拟机代码的数组
    String word[] = new String[norw];    // 保留字
    symbol wsym[] = new symbol[norw];    // 保留字对应的符号值
    symbol ssym[] = new symbol[256];     // 单字符的符号值
    char mnemonic[][] = new char[fctnum][5];     // 虚拟机代码指令名称
    boolean declbegsys[] = new boolean[sysnum];  // 表示声明开始的符号集合
    boolean statbegsys[] = new boolean[sysnum];  // 表示语句开始的符号集合
    boolean factbegsys[] = new boolean[sysnum];  // 表示因子开始的符号集合

    // 符号表结构
    class tablestruct {
        String name;  // 名字
        object kind;  // 类型 const var 或者procedure
        int val;      // 数值，仅const使用
        int level;    // 所处层，仅const不使用
        int adr;      // 地址，仅const不使用
        int size;     // 需要分配的数据区空间，仅procedure使用
    }

    tablestruct table[] = new tablestruct[txmax]; // 符号表

    InputStream fin;      // 输入源文件
    OutputStream ftable;  // 输出符号表
    OutputStream fcode;   // 输出虚拟机代码
    OutputStream foutput; // 输出文件及错误示意
    OutputStream fresult; // 输出执行结果
    String fname;
    int err; // 错误计数器

    @Override
    public void error(int n) throws IOException {

    }

    @Override
    public void getsym() throws IOException {

    }

    @Override
    public void getch() throws IOException {

    }

    @Override
    public void init() throws IOException {
        int i;
        // 设置单字符符号
        for (i = 0; i <= 255; i++) {
            ssym[i] = nul;
        }
        ssym['+'] = plus;
        ssym['-'] = minus;
        ssym['*'] = times;
        ssym['/'] = slash;
        ssym['('] = lparen;
        ssym[')'] = rparen;
        ssym['='] = eql;
        ssym[','] = comma;
        ssym['.'] = period;
        ssym['#'] = neq;
        ssym[';'] = semicolon;

        // 设置保留字名字，按照字母顺序，便于二分查找
        word[0] = "begin";
        word[1] = "call";
        word[2] = "const";
        word[3] = "do";
        word[4] = "end";
        word[5] = "if";
        word[6] = "odd";
        word[7] = "procedure";
        word[8] = "read";
        word[9] = "then";
        word[10] = "var";
        word[11] = "while";
        word[12] = "write";

        // 设置保留字符号
        wsym[0] = beginsym;
        wsym[1] = callsym;
        wsym[2] = constsym;
        wsym[3] = dosym;
        wsym[4] = endsym;
        wsym[5] = ifsym;
        wsym[6] = oddsym;
        wsym[7] = procsym;
        wsym[8] = readsym;
        wsym[9] = thensym;
        wsym[10] = varsym;
        wsym[11] = whilesym;
        wsym[12] = writesym;

        // 设置符号集
        for (i = 0; i < sysnum; i++) {
            declbegsys[i] = false;
            statbegsys[i] = false;
            factbegsys[i] = false;
        }

        // 设置声明开始符号集
        declbegsys[constsym.ordinal()] = true;
        declbegsys[varsym.ordinal()] = true;
        declbegsys[procsym.ordinal()] = true;

        // 设置语句开始符号集
        statbegsys[beginsym.ordinal()] = true;
        statbegsys[callsym.ordinal()] = true;
        statbegsys[ifsym.ordinal()] = true;
        statbegsys[whilesym.ordinal()] = true;

        // 设置因子开始符号集
        factbegsys[ident.ordinal()] = true;
        factbegsys[number.ordinal()] = true;
        factbegsys[lparen.ordinal()] = true;
    }

    /**
     * 生成虚拟机代码
     *
     * @param x  instruction.f
     * @param y  instruction.l
     * @param z  instruction.a
     */
    @Override
    public void gen(fct x, int y, int z) {
        if(cx >= cxmax) {
            System.out.println("Program is too long!"); // 生成的虚拟机代码程序过长
            System.exit(1);
        }
        if(z >= amax) {
            System.out.println("Displace address is too big");  // 地址偏移越界
            System.exit(1);
        }
        code[cx].f = x;
        code[cx].l = y;
        code[cx].a = z;
        cx++;
    }

    @Override
    public void test(boolean[] s1, boolean[] s2, int n) throws IOException {

    }

    @Override
    public boolean inset(int e, boolean[] s) throws IOException {
        return false;
    }

    @Override
    public int addset(boolean[] sr, boolean[] s1, boolean[] s2, int n) throws IOException {
        return 0;
    }

    @Override
    public int subset(boolean[] sr, boolean[] s1, boolean[] s2, int n) throws IOException {
        return 0;
    }

    @Override
    public int mulset(boolean[] sr, boolean[] s1, boolean[] s2, int n) throws IOException {
        return 0;
    }

    @Override
    public void block(int lev, int tx, boolean[] fsys) throws IOException {

    }

    @Override
    public void interpret() {

    }

    @Override
    public void factor(boolean[] fsys, Integer ptx, int lev) throws IOException {

    }

    @Override
    public void term(boolean[] fsys, Integer ptx, int lev) throws IOException {

    }

    @Override
    public void condition(boolean[] fsys, Integer ptx, int lev) throws IOException {

    }

    @Override
    public void expression(boolean[] fsys, Integer ptx, int lev) throws IOException {

    }

    @Override
    public void statement(boolean[] fsys, Integer ptx, int lev) throws IOException {

    }

    @Override
    public void listcode(int cx0) {

    }

    @Override
    public void listall() {

    }

    @Override
    public void vardeclaration(Integer ptx, int lev, Integer pdx) throws IOException {

    }

    @Override
    public void constdeclaration(Integer ptx, int lev, Integer pdx) throws IOException {

    }

    @Override
    public int position(String idt, int tx) throws IOException {
        return 0;
    }

    @Override
    public void enter(object k, Integer ptx, int lev, Integer pdx) throws IOException {

    }

    @Override
    public int base(int l, Integer s, int b) {
        return 0;
    }
}
