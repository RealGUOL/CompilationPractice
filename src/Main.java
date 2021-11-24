import java.io.*;
import java.util.Scanner;

public class Main implements Mth {

    int norw = 13;    // 保留字个数
    int txmax = 100;  // 符号表容量
    int nmax = 14;    // 数字的最大位数
    int al = 10;      // 标识符的最大长度

    enum symbol {
        nul, ident, number, plus, minus,
        times, slash, oddsym, eql, neq,
        lss, leq, gtr, geq, lparen,
        rparen, comma, semicolon, period, becomes,
        beginsym, endsym, ifsym, thensym, whilesym,
        writesym, readsym, dosym, callsym, constsym,
        varsym, procsym,
    }

    int symnum = 32;

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

    InputStream fin;      // 输入源文件
    OutputStream foutput; // 输出文件及错误示意

    // 符号表结构
    class tablestruct {
        String name;  // 名字
        object kind;                 // 类型 const var 或者procedure
    }

    tablestruct table[] = new tablestruct[txmax]; // 符号表

    public void main(String[] args) throws IOException {
        System.out.println("Input pl/0 file");
        Scanner sc = new Scanner(System.in);
        String fname = sc.nextLine();
        File file = new File(fname);
        fin = new FileInputStream(file);
        foutput = new FileOutputStream(new File("output.txt"));


        init();  // 初始化
        cc = ll = 0;
        ch = ' ';
        getsym();

        block(0);   // 处理分程序

        if (sym != symbol.period) {
            error(9);
        } else {
            System.out.println("==========Parsing success==========");
            foutput.write("==========Parsing success==========".getBytes());
        }

        fin.close();
        foutput.close();
    }

    // 出错处理，打印出错位置和错误编码
    // 遇到错误就退出语法分析
    public void error(int n) {
        char space[] = new char[81];
        for (int i = 0; i < 81; i++) {
            space[i] = 32;
        }
        space[cc - 1] = 0;  // 出错时当前符号已经读完，所以cc-1
        System.out.printf("%s^%d\n", space, n);
        System.exit(1);
    }

    // 词法分析，获得一个符号
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
            } else { // 当前字符是表示符
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

    // 过滤空格，读取一个字符
    // 每次读一行，存入line缓冲区，line被getsym取空后再读一行
    // 被函数getsym调用
    public void getch() throws IOException {
        if (cc == ll) {
            if (fin == null) {
                System.out.println("Program imcomplete!");
                System.exit(1);
            }
            cc = 0;
            ll = 0;

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

    // 初始化
    public void init() {
        int i;
        // 设置单字符符号
        for (i = 0; i <= 255; i++) {
            ssym[i] = symbol.nul;
        }
        ssym['+'] = symbol.plus;
        ssym['-'] = symbol.minus;
        ssym['*'] = symbol.times;
        ssym['/'] = symbol.slash;
        ssym['('] = symbol.lparen;
        ssym[')'] = symbol.rparen;
        ssym['='] = symbol.eql;
        ssym[','] = symbol.comma;
        ssym['.'] = symbol.period;
        ssym['#'] = symbol.neq;
        ssym[';'] = symbol.semicolon;

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
        wsym[0] = symbol.beginsym;
        wsym[1] = symbol.callsym;
        wsym[2] = symbol.constsym;
        wsym[3] = symbol.dosym;
        wsym[4] = symbol.endsym;
        wsym[5] = symbol.ifsym;
        wsym[6] = symbol.oddsym;
        wsym[7] = symbol.procsym;
        wsym[8] = symbol.readsym;
        wsym[9] = symbol.thensym;
        wsym[10] = symbol.varsym;
        wsym[11] = symbol.whilesym;
        wsym[12] = symbol.writesym;
    }

    // 编译程序主体
    // tx: 符号表当前尾指针
    public void block(Integer tx) throws IOException {
        int i;
        if (sym == symbol.constsym) {
            getsym();
            constdeclaration(tx);
            while (sym == symbol.comma) { // 遇到都好继续定义常量
                getsym();
                constdeclaration(tx);
            }
            if (sym == symbol.semicolon) { // 遇到分号结束定义常量
                getsym();
            } else {
                error(5); // 漏掉了分号
            }
        }

        if (sym == symbol.varsym) {  // 遇到变量声明符号，开始处理变量声明
            getsym();
            vardeclaration(tx);
            while (sym == symbol.comma) {
                getsym();
                vardeclaration(tx);
            }
            if (sym == symbol.semicolon) {
                getsym();
            } else {
                error(5);  // 漏掉分号
            }
        }

        while (sym == symbol.procsym) {
            getsym();
            if (sym == symbol.ident) {
                enter(object.procedure, tx);  // 开始填写符号表
                getsym();
            } else {
                error(4); // procedure 后应为表示符
            }
            if (sym == symbol.semicolon) {
                getsym();
            } else {
                error(5);  // 漏掉了分号
            }
            block(tx); // 递归调用
            if (sym == symbol.semicolon) {
                getsym();
            } else {
                error(5);  // 漏掉了分号
            }
        }
        statement(tx);

    }

    // 因子处理
    public void factor(Integer ptx) throws IOException {
        int i;
        if (sym == symbol.ident) {
            i = posision(id, ptx);
            if (i == 0) {
                error(11); // 标识符未声明
            } else {
                if (table[i].kind == object.procedure) {
                    error(21); // 不能为进程
                }
            }
            getsym();
        } else {
            if (sym == symbol.number) {
                getsym();
            } else {
                if (sym == symbol.lparen) {
                    getsym();
                    expression(ptx);
                    if (sym == symbol.rparen) {
                        getsym();
                    } else {
                        error(22); // 缺少有括号
                    }
                } else {
                    error(23);
                }
            }
        }
    }

    // 项处理
    public void term(Integer ptx) throws IOException {
        factor(ptx);

        while (sym == symbol.times || sym == symbol.slash) {
            getsym();
            factor(ptx);
        }
    }

    // 条件处理
    public void condition(Integer ptx) throws IOException {
        if (sym == symbol.oddsym) {
            getsym();
            expression(ptx);
        } else {
            expression(ptx);
            if (sym != symbol.eql && sym != symbol.neq && sym != symbol.lss &&
                    sym != symbol.leq && sym != symbol.gtr && sym != symbol.geq) {
                error(20); // 应该为关系运算符
            } else {
                getsym();
                expression(ptx);
            }
        }
    }

    // 表达式处理
    public void expression(Integer ptx) throws IOException {
        if (sym == symbol.plus || sym == symbol.minus) {
            getsym();
            term(ptx);
        } else {
            term(ptx);
        }
        while (sym == symbol.plus || sym == symbol.minus) {
            getsym();
            term(ptx);
        }
    }

    // 语句处理
    public void statement(Integer ptx) throws IOException {
        int i;
        if (sym == symbol.ident) { // 准备按照赋值语句处理
            i = posision(id, ptx);   // 查找标识符在符号表中的位置
            if (i == 0) {
                error(11); // 标识符未声明
            } else {
                if (table[i].kind != object.variable) {
                    error(12);  // 赋值语句中，赋值号左部标识符应该是变量
                    i = 0;
                } else {
                    getsym();
                    if (sym == symbol.becomes) {
                        getsym();
                    } else {
                        error(13); // 没有检测到赋值符号
                    }
                    expression(ptx);
                }
            }
        } else {
            if (sym == symbol.callsym) {  // 准备按照call语句处理
                getsym();
                if (sym != symbol.ident) {
                    error(14);  // call后应该未标识符
                } else {
                    i = posision(id, ptx);
                    if (i == 0) {
                        error(11); // 过程名未找到
                    } else {
                        if (table[i].kind != object.procedure) {
                            error(15);  // call后标识符种类应为过程
                        }
                    }
                    getsym();
                }
            } else {
                if (sym == symbol.ifsym) {
                    getsym();
                    condition(ptx);
                    if (sym == symbol.thensym) {
                        getsym();
                    } else {
                        error(16);  // 缺少then
                    }
                    statement(ptx);
                } else {
                    if (sym == symbol.beginsym) {
                        getsym();
                        statement(ptx);

                        while (sym == symbol.semicolon) {
                            getsym();
                            statement(ptx);
                        }
                        if (sym == symbol.endsym) {
                            getsym();
                        } else {
                            error(17); // 缺少end
                        }
                    } else {
                        if (sym == symbol.whilesym) {
                            getsym();
                            condition(ptx);
                            if (sym == symbol.dosym) {
                                statement(ptx);
                            } else {
                                error(18); // 缺少do
                            }
                            statement(ptx); // 循环体
                        }
                    }
                }
            }
        }
    }

    // 变量声明处理
    public void vardeclaration(Integer ptx) throws IOException {
        if (sym == symbol.ident) {
            enter(object.variable, ptx);
            getsym();
        } else {
            error(4);  // var后面应该是标识符
        }
    }

    // 常量声明处理
    public void constdeclaration(Integer ptx) throws IOException {
        if (sym == symbol.ident) {
            getsym();
            if (sym == symbol.eql) {
                getsym();
                if (sym == symbol.number) {
                    enter(object.constant, ptx);
                    getsym();
                } else {
                    error(2);  // 常量声明中的=后应该是数字
                }
            } else {
                error(3); // 常量声明中的标识符应该是=
            }
        } else {
            error(4);   // const后面应该是标识符
        }
    }

    // id: 要查找的名字
    // tx: 当前符号表的尾指针
    public int posision(String id, Integer tx) {
        int i;
        table[0].name = id; // 这一步有必要吗？·
        i = tx;
        while (id.compareTo(table[i].name) != 0) {
            i--;
        }
        return i;

    }

    // 在符号表中加入一项
    // k: 表示符的种类为const var 或 procedure
    // ptx: 符号表尾指针的指针，为了可以改变符号表尾指针的值
    public void enter(object k, Integer ptx) {
        ptx++;
        table[ptx].name = id;
        table[ptx].kind = k;
    }


}
