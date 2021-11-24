package dealerror;

import org.w3c.dom.ls.LSOutput;

import java.io.*;
import java.util.Arrays;
import java.util.Scanner;

import static dealerror.Main.symbol.*;

public class Main implements Mth {

    int norw = 13;    // 保留字个数
    int txmax = 100;  // 符号表容量
    int nmax = 14;    // 数字的最大位数
    int al = 10;      // 标识符的最大长度
    int maxerr = 30;  // 允许的最多错误数

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

    char ch;    // 存放当前读取的字符，getch使用
    symbol sym;  // 当前的符号
    String id; // 当前ident，多出的一个字符用于存放0
    int num;    // 当前的number
    int cc, ll;  // getch 使用的计数器，cc表示当前字符(ch)的位置
    char line[] = new char[81]; // 读取行缓冲区
    String a;  // 临时符号，多出的一个字节用于存放0
    String word[] = new String[norw];    // 保留字
    symbol wsym[] = new symbol[norw];    // 保留字对应的符号值
    symbol ssym[] = new symbol[256];     // 单字符的符号值
    boolean declbegsys[] = new boolean[sysnum];  // 表示声明开始的符号集合
    boolean statbegsys[] = new boolean[sysnum];  // 表示语句开始的符号集合
    boolean factbegsys[] = new boolean[sysnum];  // 表示因子开始的符号集合

    // 符号表结构
    class tablestruct {
        String name;  // 名字
        object kind;                 // 类型 const var 或者procedure
    }

    tablestruct table[] = new tablestruct[txmax]; // 符号表

    InputStream fin;      // 输入源文件
    OutputStream foutput; // 输出文件及错误示意
    String fname;
    int err; // 错误计数器

    int main() throws IOException {
        boolean nxtlev[] = new boolean[sysnum];  // 跟随符号集

        Scanner sc = new Scanner(System.in);
        System.out.println("Input pl/0 file?");
        fname = sc.nextLine();
        File file = new File(fname);
        fin = new FileInputStream(file);
        foutput = new FileOutputStream(new File("output.txt"));

        init(); // 初始化
        err = 0;
        cc = ll = 0;
        ch = ' ';

        getsym();
        addset(nxtlev, declbegsys, statbegsys, sysnum);
        nxtlev[period.ordinal()] = true;
        block(0, nxtlev); // 处理分程序

        if (sym != period) {
            error(9);
        }
        if (err == 0) {
            System.out.println("===Parsing Success!===");
            foutput.write("===Parsing Success!===\n".getBytes());
        } else {
            System.out.println("===" + err + " errors in pl/0 program!===");
            foutput.write(("===" + err + " errors in pl/0 program!===\n").getBytes());
        }

        foutput.close();
        fin.close();

        return 0;
    }

    @Override
    public void error(int n) throws IOException {
        char space[] = new char[81];
        for (int i = 0; i < 81; i++) {
            space[i] = 32;
        }
        space[cc - 1] = 0;  // 出错时当前符号已经读完，所以cc-1
        System.out.printf("%s^%d\n", space, n);
        foutput.write(System.out.format("%s^%d\n", space, n).toString().getBytes());
        err++;
        if(err > maxerr) {
            System.exit(1);
        }
    }

    @Override
    public void getsym() throws IOException {
        int i, j, k;

        while (ch == ' ' || ch == 10 || ch == 9) { // 过滤空格、换行、制表符
            getch();
        }

        if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {  // 当前的单词是表示符或是保留字
            a = "";
            k = 0;
            do {
                if (k < al) {
                    a += ch;
                    k++;
                }
                getch();
            } while ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9'));
            id = a;
            i = 0;
            j = norw - 1;
            do {  // 搜索当前单词是否为保留字，使用二分查找
                k = (i + j) / 2;
                if (id.compareTo(word[k]) <= 0) {
                    j = k - 1;
                } else if (id.compareTo(word[k]) >= 0) {
                    i = k + 1;
                }
            } while (i <= j);
            if (i - 1 > j) { // 当前字符是保留字
                sym = wsym[k];
            } else { // 当前字符是标识符
                sym = symbol.ident;
            }
        } else if (ch >= '0' && ch <= '9') {  // 判断是否是数字
            k = 0;
            num = 0;
            sym = symbol.number;
            do {
                num = 10 * num + ch - '0';
                k++;
                getch();
            } while (ch >= '0' && ch <= '9');
            k--;
            if (k > nmax) { // 数字位数太多
                error(30);
            }
        } else {
            if (ch == ':') {  // 检测赋值符号
                getch();
                if (ch == '=') {
                    sym = symbol.becomes;
                    getch();
                } else {
                    sym = symbol.nul; // 不能识别符号
                }
            } else {
                if (ch == '<') {  // 检测大于或大于等于号
                    getch();
                    if (ch == '=') {
                        sym = symbol.leq;
                        getch();
                    } else {
                        sym = symbol.lss;
                    }
                } else {
                    if (ch == '>') { // 检查大于或大于等于符号
                        getch();
                        if (ch == '=') {
                            sym = symbol.geq;
                            getch();
                        } else {
                            sym = symbol.gtr;
                        }
                    } else {
                        sym = ssym[ch]; // 当符号不满足上述条件时，全部按照单字符符号处理
                        if (sym != symbol.period) {
                            getch();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void getch() throws IOException {
        if(cc == ll) {
            if (fin == null) {
                System.out.println("Program imcomplete!");
                System.exit(1);
            }
            ll = 0;
            cc = 0;

            ch = ' ';
            while (ch != 10) {   // 换行符10
                int t = fin.read();
                if (t == -1) {
                    line[ll] = 0;
                    break;
                }
                ch = (char) t;
                System.out.println(ch);
                foutput.write(ch);
                line[ll] = ch;
                ll++;
            }
        }
        ch = line[cc];
        cc++;
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
     * 测试当前符号是否合法
     *
     * 在语法分析程序的入口和出口处调用测试代码test
     * 检查当前单词进入和推出该语法单位的合法性
     *
     * @param s1 需要的单词集合
     * @param s2 如果不是需要的单词，在某一出错状态时，可以恢复语法分析继续正常工作的补充单词符号集合
     * @param n 错误号
     * @throws IOException
     */
    @Override
    public void test(boolean[] s1, boolean[] s2, int n) throws IOException {
        if(!inset(sym.ordinal(), s1)) {
            error(n);
            // 当检测不通过时，不停获取符号，直到它属于需要的集合或补救的集合
            while(!inset(sym.ordinal(), s1) && !inset(sym.ordinal(), s2)) {
                getsym();
            }
        }
    }

    @Override
    public boolean inset(int e, boolean[] s) throws IOException {
        return s[e];
    }

    @Override
    public int addset(boolean[] sr, boolean[] s1, boolean[] s2, int n) throws IOException {
        int i;
        for(i = 0; i < n; i++) {
            sr[i] = s1[i] || s2[i];
        }
        return 0;
    }

    @Override
    public int subset(boolean[] sr, boolean[] s1, boolean[] s2, int n) throws IOException {
        int i;
        for(i = 0; i < n; i++) {
            sr[i] = s1[i] && !s2[i];
        }
        return 0;
    }

    @Override
    public int mulset(boolean[] sr, boolean[] s1, boolean[] s2, int n) throws IOException {
        int i;
        for(i = 0; i < n; i++) {
            sr[i] = s1[i] && s2[i];
        }
        return 0;
    }

    /**
     * 编译程序主体
     *
     * @param tx 符号表当前尾指针
     * @param fsys 当前模块后记符号表集合
     * @throws IOException
     */
    @Override
    public void block(int tx, boolean[] fsys) throws IOException {
        int i;
        // 在下级函数的参数中，符号集合均为值参，但由于使用数组实现，传递进来的是指针，为了防止下级函数改变上级函数集合，开辟新的空间传递给下级函数
        boolean nxtlev[];

        do{
            if(sym == constsym) {  // 遇到常量声明符号
                getsym();
                do {
                    constdeclaration(tx);
                    while(sym == comma) {
                        getsym();
                        constdeclaration(tx);
                    }
                    if(sym == semicolon) {
                        getsym();
                    } else {
                        error(5);
                    }
                }while(sym == ident);
            }
            if(sym == varsym) { // 遇到变量声明符号
                getsym();
                do {
                    vardeclaration(tx);
                    while (sym == comma) {
                        getsym();
                        vardeclaration(tx);
                    }
                    if(sym == semicolon) {
                        getsym();
                    }else{
                        error(5);
                    }
                } while (sym == ident);
            }
            while(sym == procsym) {  // 遇到过程声明符号
                getsym();
                if(sym == ident) {
                    enter(object.procedure, tx);
                    getsym();
                }else {
                    error(4);
                }

                if(sym == semicolon) {
                    getsym();
                }else {
                    error(5);
                }
                nxtlev = Arrays.copyOf(fsys, sysnum);
                nxtlev[semicolon.ordinal()] = true;
                block(tx, nxtlev);
                if(sym == semicolon) {  // 这一步没有看懂 P216
                    getsym();
                    nxtlev = Arrays.copyOf(statbegsys, sysnum);
                    nxtlev[ident.ordinal()] = true;
                    nxtlev[procsym.ordinal()] = true;
                    test(nxtlev, fsys, 6);
                } else {
                    error(5);  // 漏掉了分号
                }
            }
            nxtlev = Arrays.copyOf(statbegsys, sysnum);
            nxtlev[ident.ordinal()] = true;
            test(nxtlev, declbegsys, 7);
        } while(inset(sym.ordinal(), declbegsys));  // 直到没有声明符号

        // 语句后继符号为分号或end

        // 每个后记符号集合都包含上层后记符号集合，以便补救
        nxtlev = Arrays.copyOf(fsys, sysnum);
        nxtlev[semicolon.ordinal()] = true;
        nxtlev[endsym.ordinal()] = true;
        statement(nxtlev, tx);
        nxtlev = new boolean[sysnum];   // 分程序没有补救集合
        test(fsys, nxtlev, 8); // 检测后记符号正确性
    }

    @Override
    public void factor(boolean[] fsys, Integer ptx) throws IOException {

    }

    @Override
    public void term(boolean[] fsys, Integer ptx) throws IOException {

    }

    @Override
    public void condition(boolean[] fsys, Integer ptx) throws IOException {

    }

    @Override
    public void expression(boolean[] fsys, Integer ptx) throws IOException {

    }

    @Override
    public void statement(boolean[] fsys, Integer ptx) throws IOException {

    }

    @Override
    public void vardeclaration(Integer ptx) throws IOException {

    }

    @Override
    public void constdeclaration(Integer ptx) throws IOException {

    }

    @Override
    public int position(String idt, int tx) throws IOException {
        return 0;
    }

    /**
     * 在符号表中加入一项
     * @param k 标识符的种类为const，var或procedure
     * @param ptx 符号表尾指针的指针，为了可以改变符号表尾指针的值
     * @throws IOException
     */
    @Override
    public void enter(object k, Integer ptx) throws IOException {
        ptx++;
        table[ptx].name = id;
        table[ptx].kind = k;
    }
}
