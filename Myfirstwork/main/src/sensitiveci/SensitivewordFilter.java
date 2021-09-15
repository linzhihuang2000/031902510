package sensitiveci;

import net.sourceforge.pinyin4j.PinyinHelper;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("all")
public class SensitivewordFilter {

    public static Map sensitiveWordMap = null; //敏感词词库，DFA算法储存
    private static String ans="";
    private static String an="";//答案
    private static int total=0;
    private static String regEx = "[`~!@#$%^&*()+-=|{}':;,\\[\\].<>/?！￥…（）—【】‘；：’。，_·、？ \"\'0123456789]";//特殊符号+数字
    private static String lowercase="abcdefghijklmnopqrstuvwxyz";//小写字母
    private static String uppercase="ABCDEFGHIJKLMNOPQRSTUVWXYZ";//大写字母
    private static String moderncase ="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";//字母
    private static String yumu="aAuUIiEeOo";//韵母

    //以下四行，等检测拼音和拼音首字母的时候再用
    private static String string="";//都是拼音字符串
    private static Map tempstringMap=sensitiveWordMap;//暂时的map
    private static String tempstringsensitiveci="";//暂时的当前检测的敏感词库
    private static String tempstringnowstring="";//暂时的当前所检测到的敏感词

    //
    public static void main(String[] args) {
        Set<String> set1 = new HashSet<String>();
        splitci();
        set1 = readfile(args[0]);//读入敏感词文件
        SensitivewordFilter filter = new SensitivewordFilter(set1);//把敏感词用DFA写入Map
        readtext(args[1]);
        sum();//把答案汇总；
        outflie(args[2]);//输出文件
    }

    //以下函数，是把敏感词用DFA算法写入
    public SensitivewordFilter(Set<String> keyWords){
        sensitiveWordMap = new SensitiveWordInit().initKeyWord(keyWords);
    }

    //以下函数，是检测文本中的敏感词
    //主要思想，例如fuck是敏感词，Fuck是文本
    //检测到F的时候，不通过，再次调用该函数，F则替换成f继续检测
    //递归
    public static int CheckSensitiveWord2(String txt,int j,int n,Map nowMap, String sensitiveci, String nowstring, boolean flag,String ti){
        //从左到右的参数是，文本，文本已经读到的下标，文本是第几行，当前敏感词库，
        // 当前已经读到的敏感词库中的词，当前读到的敏感词，是否被替换false表示没有替换
        //当前替换的词
        String word="";
        int start=-1;//一串纯英文加特殊符号在txt的下标
        int end=0;
        for(int i = j; i < txt.length() ; i++) {
            if (!flag)
                word = txt.substring(i, i + 1);
            else
                word=ti;//true表示当前检测的，是被替换的
            if (regEx.indexOf(word) != -1) {//特殊字符直接跳过
                if (!nowstring.equals(""))
                    nowstring += word;//nowstring不为空，说明当前已找到敏感词的部分内容
                continue;
            }

            if(!flag)
            {
                if(moderncase.indexOf(word)!=-1)//是不是字母
                {
                    if(string.equals("")) {
                        start = i;
                        tempstringMap=nowMap;
                        tempstringnowstring=nowstring;
                        tempstringsensitiveci=sensitiveci; //把当前参数存起来，为找 变成拼音 的敏感词
                    }
                    string+=word;
                }
                //当检测到汉字或是最后一个字符，string（一串纯英文）去检测拼音敏感词
                if((ishan(word)||i==txt.length()-1)&&!string.equals("")&&start!=-1)
                {
                    if(i==txt.length()-1&&moderncase.indexOf(word)!=-1)
                        end=i;
                    else
                        end=i-1;
                    if(Checkstring(txt,start,end,n)==1)//检测拼音敏感词
                    {
                        //返回值为1，说明这串英语字符，是拼音敏感词
                        nowMap=tempstringMap;
                        nowstring=tempstringnowstring+txt.substring(start,end+1);
                        sensitiveci=tempstringsensitiveci;
                    }
                    string="";
                }
            }
            Map tempMap = nowMap;
            nowMap = (Map) nowMap.get(word);//获取指定key
            if (nowMap != null) {//存在，则判断是否为最后一个
                if (!flag)
                    nowstring += word;
                sensitiveci += word;
                flag = false;
                if ("1".equals(nowMap.get("isEnd"))) {//如果为最后一个匹配规则,结束循环，返回匹配标识数
                    total++;
                    an += "Line" + n + ":" + " <" + sensitiveci + "> " + nowstring + "\n";
                    //初始化
                    nowMap = sensitiveWordMap;
                    sensitiveci="";
                    nowstring="";
                    string="";
                    flag=false;
                    ti="";
                    //找完找下一个
                    CheckSensitiveWord2(txt,i+1,n,nowMap,sensitiveci,nowstring,flag,ti);
                    return 1;
                }
            }
            //该字符是英文，但是检测不成功
            else if(moderncase.indexOf(word)!=-1&&!flag)
            {
                int pan=2;
                String tempstring=nowstring;
                if(uppercase.indexOf(word)!=-1)//是小写
                {
                    flag = true;
                    nowstring += word;
                    word = word.toLowerCase();//改为大写再次检测
                    ti=word;
                    nowMap = tempMap;
                    pan=CheckSensitiveWord2(txt,i,n,nowMap,sensitiveci,nowstring,flag,ti);
                }
                else//大写的话，改为小写去检测
                {
                    flag = true;
                    nowstring += word;
                    word = word.toUpperCase();
                    ti=word;
                    nowMap = tempMap;
                    pan=CheckSensitiveWord2(txt,i,n,nowMap,sensitiveci,nowstring,flag,ti);
                }
                if(pan==1) {//pan为1，说明被替换的字母正是我们要找的
                    string = "";
                    return 1;
                }
                //否则，就初始化
                    sensitiveci = "";
                    nowstring = "";
                    nowMap = sensitiveWordMap;
                    flag = false;
            }
            //字符是汉字，但是检测不成功，可能是谐音，或者左右结构拆开的原因（强=弓虽）
            else if(ishan(word) && !flag)
            {
                int pan=2;//用来判断的
                String word2;
                if (i != txt.length() - 1)//左右结构拆开
                {
                    word2 = txt.substring(i + 1, i + 2);
                    if (ishan(word2)) {
                    String word3 = word;
                    word3 += word2;
                    if (split.containsKey(word3)) {//如果改字是被拆开的，那把他组合起来再去检测
                        flag = true;
                        nowstring += word3;
                        word = split.get(word3);
                        nowMap = tempMap;
                        pan=CheckSensitiveWord2(txt,i+1,n,nowMap,sensitiveci,nowstring,flag,word);
                    }
                    if(pan==1)//pan为1，说明组合起来的字去检测是成功的
                    return 1;
                   }
                }
                //谐音问题
                String wordpinyin=hanzitopinyin(word);//把该字转换为拼音
                if(pinyintohanzi.containsKey(wordpinyin))//如果该拼音的确是敏感词
                {
                    String temp=pinyintohanzi.get(wordpinyin);//一串是谐音的敏感词字符串，例如（fa），该字符串就是发法罚....
                    flag = true;
                    nowstring += word;
                    nowMap = tempMap;
                        for (int z=0;z<temp.length();z++)//一个一个谐音去试
                        {
                            word=temp.substring(z,z+1);
                            pan=CheckSensitiveWord2(txt,i,n,nowMap,sensitiveci,nowstring,flag,word);
                            if(pan==1)
                                return 1;
                        }
                }
                sensitiveci = "";
                nowstring = "";
                nowMap = sensitiveWordMap;
                flag = false;
            }
            //该字符不能被替换，或已经被替换过了
            else {
                if(flag)
                    return -1;//-1表示替换了，也不成功
                sensitiveci = "";
                nowstring = "";
                nowMap = sensitiveWordMap;
                flag = false;
            }
        }
        return 2;//2表示程序正常结束
    }


    //检测拼音敏感词
    public static int Checkstring(String txt,int start,int end,int n)
    {
        //如果当前敏感词库的词是英语，则重置
        if(moderncase.indexOf(tempstringsensitiveci)!=-1)
        {
            tempstringnowstring="";
            tempstringMap=sensitiveWordMap;
            tempstringsensitiveci="";
        }
        string="";
        int flag2=0;//flag2为1：该字符串确实为拼音敏感词，但是不到敏感词最后一个字。
        //flag为0，不是拼音敏感词
        for(int i=start;i<end+1;i++)
        {
            if (moderncase.indexOf(txt.substring(i,i+1))!=-1)//是字母
            string+=txt.substring(i,i+1);
            //判断下一个是不是韵母
            if(string.length()==1&&i!=end&&yumu.indexOf(txt.substring(i+1,i+2))!=-1)
            {
                i++;
                string+=txt.substring(i,i+1);
            }
            string=string.toLowerCase();
            //改成小写，因为我Map函数是小写对应汉字的
            if(pinyintohanzi.containsKey(string))//有该敏感词拼音
            {
                String gg=pinyintohanzi.get(string);//谐音集合
                for(int z=0;z<gg.length();z++)//一个一个谐音去试
                {
                    String hh=gg.substring(z,z+1);
                    if(tempstringMap.containsKey(hh))//检测成功，加入
                    {
                        tempstringMap=(Map)tempstringMap.get(hh);
                        tempstringsensitiveci+=hh;
                        flag2=i;
                        if("1".equals(tempstringMap.get("isEnd")))//是敏感词的最后一个字
                        {
                            tempstringnowstring+=txt.substring(start,i+1);
                            total++;
                            an += "Line" + n + ":" + " <" + tempstringsensitiveci + "> " +tempstringnowstring + "\n";
                            string="";
                            return 0;
                        }
                        string="";
                        break;
                    }
                }
            }
        }
        start=-1;
        if (flag2==end)//说明检测成功，但还不是敏感词最后一字
        {
            return 1;
        }
        return -1;
    }

    //初始化
    private static void initialization()
    {
        String string="";
        Map tempstringMap=sensitiveWordMap;
        String tempstringsensitiveci="";
        String tempstringnowstring="";
    }





    //判断是不是汉字
    public static boolean ishan(String word)
    {
        Pattern pattern = Pattern.compile("[\\u4E00-\\u9FA5]");
        Matcher matcher = pattern.matcher(word);
        return matcher.find();
    }







    private static Map<String,String> pinyintohanzi=new HashMap<String, String>();
    //拼音转汉字
    private static void pinyin(String gg)
    {
        for(int i=0;i<gg.length();i++)
        {
            char temp=gg.charAt(i);
            String pin []= PinyinHelper.toHanyuPinyinStringArray(temp);
            String pin1=pin[0].substring(0,1);//拼音首字母
            String pin4=pin[0].substring(0,pin[0].length()-1);//拼音不含音标
            if(!pinyintohanzi.containsKey(pin1))
            {
                pinyintohanzi.put(pin1,""+temp);
            }
            else
            {
                pinyintohanzi.put(pin1,pinyintohanzi.get(pin1)+temp);
            }
            if(!pinyintohanzi.containsKey(pin4))
            {
                pinyintohanzi.put(pin4,""+temp);
            }
            else
            {
                pinyintohanzi.put(pin4,pinyintohanzi.get(pin4)+temp);
            }
        }
    }




    //汉字变成拼音
    private static String hanzitopinyin(String hh)
    {
        char temp=hh.charAt(0);
        String pin []= PinyinHelper.toHanyuPinyinStringArray(temp);
        String pinyin=pin[0].substring(0,pin[0].length()-1);
        return pinyin;
    }






    //拆分汉字
    public  static Map<String,String> split =new HashMap<String, String>();//拆分汉字
    public  static void splitci()
    {
        split= splitcihui.splitci();
    }




    //答案汇总
    public static void sum()
    {
        ans="Total："+total+"\n";
        ans=ans+an;
    }







//以下为读入敏感词词汇
    public static Set<String> readfile(String readf) {
        Set<String> set1 = new HashSet<>();
        try {
            File f = new File(readf);//指定文件
            FileInputStream fis = new FileInputStream(f);//创建输入流fis并以f为参数
            InputStreamReader isr = new InputStreamReader(fis,"UTF-8");//创建字符输入流对象isr并以fis为参数
            BufferedReader br = new BufferedReader(isr);//创建一个带缓冲的输入流对象br，并以isr为参数
            String gg;
            while((gg=br.readLine())!=null)
            {
                set1.add(gg);
                if(ishan(gg))
                    pinyin(gg);
            }
        } catch (Exception e) {
            System.out.println("找不到该文件");
            e.printStackTrace();
        }
        return  set1;
    }



    //以下为读入检测的文本
    private static void readtext(String readt) {
        try {
            File f = new File(readt);//指定文件
            FileInputStream fis = new FileInputStream(f);//创建输入流fis并以f为参数
            InputStreamReader isr = new InputStreamReader(fis,"UTF-8");//创建字符输入流对象isr并以fis为参数
            BufferedReader br = new BufferedReader(isr);//创建一个带缓冲的输入流对象br，并以isr为参数
            String gg;
            int i=1;//代表当前检测的是第几行
            while((gg=br.readLine())!=null)
            {
                Map nowMap = sensitiveWordMap;
                String sensitiveci="";
                String nowstring="";
                boolean flag=false;//当前检测的词是文本原来的，还是被替换的。false原来的，true替换的
                String ti="";//替换的词
                CheckSensitiveWord2(gg,0,i,nowMap,sensitiveci,nowstring,flag,ti);
                initialization();
                i++;
            }
        } catch (Exception e) {
            System.out.println("找不到该文件");
            e.printStackTrace();
        }
    }



    //以下为输出文件
    public static void outflie(String outf)
    {
        try {
            File f=new File(outf);//指定文件
            FileOutputStream fos=new FileOutputStream(f);//创建输出流fos并以f为参数
            OutputStreamWriter osw=new OutputStreamWriter(fos,"UTF-8");//创建字符输出流对象osw并以fos为参数
            BufferedWriter bw=new BufferedWriter(osw);//创建一个带缓冲的输出流对象bw，并以osw为参数
            bw.write(ans);//使用bw写入一行文字，为字符串形式String
            bw.close();//关闭并保存
        } catch (IOException e) {
            System.out.println("找不到该文件");
            e.printStackTrace();
        }
    }
}
