package sensitiveci;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

//这段程序的作用是把敏感词用DFA算法，存在sensitiveWordMap(HashMap)中;
@SuppressWarnings("all")
public class SensitiveWordInit {
    public HashMap sensitiveWordMap;

    public SensitiveWordInit(){
        super();
    }

    public Map initKeyWord(Set<String> keyWordSet){
        try {
            //将敏感词库加入到HashMap中
            addSensitiveWordToHashMap(keyWordSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sensitiveWordMap;
    }

    /**
     * 读取敏感词库，将敏感词放入HashSet中，构建一个DFA算法模型，
     * 假设敏感词是（你是傻逼，你是笨蛋，无话可说）,
     * 你 = {
     *      isEnd = 0
     *      是 = {
     *      	 isEnd = 1
     *           傻 = {isEnd = 0
     *               比 = {isEnd = 1}
     *                }
     *           笨  = {
     *           	   isEnd = 0
     *           		蛋 = {
     *           			 isEnd = 1
     *           			}
     *           	}
     *           }
     *      }
     *  无= {
     *      isEnd = 0
     *      话 = {
     *      	isEnd = 0
     *      	可 = {
     *              isEnd = 0
     *              说= {
     *                   isEnd = 1
     *                  }
     *              }
     *      	}
     *      }
     * @param keyWordSet  敏感词库
     *
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addSensitiveWordToHashMap(Set<String> keyWordSet) {
        sensitiveWordMap = new HashMap(keyWordSet.size());     //初始化敏感词容器，减少扩容操作
        String key = null;
        Map nowMap = null;
        Map<String, String> newWorMap = null;
        //迭代keyWordSet
        Iterator<String> iterator = keyWordSet.iterator();
        while(iterator.hasNext()){
            key = iterator.next();//一个词一个词地读
            nowMap = sensitiveWordMap;
            for(int i = 0 ; i < key.length() ; i++){
                String keyChar = key.substring(i,i+1);
                Object wordMap = nowMap.get(keyChar);       //获取

                if(wordMap != null){        //如果nowMap存在key，直接赋值
                    nowMap = (Map) wordMap;
                }
                else{     //不存在则，则构建一个map，同时将isEnd设置为0，因为他不是最后一个
                    newWorMap = new HashMap<String,String>();
                    newWorMap.put("isEnd", "0");     //不是最后一个
                    nowMap.put(keyChar, newWorMap);
                    nowMap = newWorMap;
                }

                if(i == key.length() - 1){
                    nowMap.put("isEnd", "1");    //最后一个
                }
            }
        }
    }

}