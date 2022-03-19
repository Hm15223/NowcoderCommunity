package com.nowcoder.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    //敏感词替换的符号
    private static final String REPLACEMENT = "**";

    //初始化根节点
    private TrieNode rootNode = new TrieNode();

    //当容器SensitiveFileter实例化bean，调用构造器之后，此初始化方法被自动调用
    @PostConstruct
    public void init() {
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));//字节流->字符流->缓冲流
        ) {
            String keyword;
            while ((keyword = reader.readLine()) != null) {
                //将此敏感词添加到前缀树中
                this.addKeyword(keyword);
            }

        } catch (IOException e) {
            logger.error("加载敏感词失败:" + e.getMessage());
        }
    }

    //向前缀树添加一个敏感词
    private void addKeyword(String keyword) {
        TrieNode tempNode = rootNode;
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);
            if (subNode == null) {
                //初始化子节点
                subNode = new TrieNode();
                //将子节点挂在当前节点之下
                tempNode.addSubNode(c, subNode);
            }
            //指向子节点，进行下一轮循环
            tempNode = subNode;
            //标记最后一个字符,设置结束标识
            if (i == keyword.length() - 1) {
                tempNode.setKeywordEnd(true);
            }
        }
    }

    /**
     * 实现检索,过滤敏感词的方法
     *
     * @param text 待过滤的文本
     * @return 过滤后的文本
     */
    public String filter(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        //指针1，指向前缀树的节点
        TrieNode tempNode = rootNode;
        //指针2，开始时指向字符串开头字符
        int begin = 0;
        //指针3，开始时指向字符串开头字符，开始检索时，向后检索有无敏感词，找到后就指向敏感词的后一个字符，否则回到开始检索字符的后一位
        int position = 0;
        //最终的结果字符串
        StringBuilder sb = new StringBuilder();

        while (position < text.length()) {
            char c = text.charAt(position);

            //跳过符号
            if(isSymbol(c)){
                //若指针1处于根节点,将此符号计入结果，指针2向后移动一步
                if(tempNode == rootNode){
                    sb.append(c);
                    begin++;
                }
                //无论符号在开头还是中间，指针3都会向后移动一步
                position++;
                continue;
            }
            //检查下级节点
            tempNode = tempNode.getSubNode(c);
            if(tempNode == null){
                //以begin开头的字符串不是敏感词
                sb.append(text.charAt(begin));
                //进入下一个位置
                position = ++begin;
                //指针1重新指向根节点
                tempNode = rootNode;
            }else if(tempNode.isKeywordEnd()){
                //发现敏感词，将begin~position字符串替换
                sb.append(REPLACEMENT);
                //进入下一个位置
                begin = ++position;
                //指针1重新指向根节点
                tempNode = rootNode;
            }else{
                //未发现敏感词，检查下一个字符
                position++;
            }
        }
        //将最后一段字符计入结果，即指针3到结尾，指针2没到结尾的情况
        sb.append(text.substring(begin));
        return sb.toString();
    }

    //判断是否为符号
    private boolean isSymbol(Character c) {
        //是否为合法符号，再取反即可,0x2E80 到 0x9FFF为东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }

    //定义前缀树
    private class TrieNode {

        //敏感词结束标识
        private boolean isKeywordEnd = false;

        //当前节点的子节点(key是下级字符，value是下级节点)
        private Map<Character, TrieNode> subNodes = new HashMap<>();


        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        //添加子节点
        public void addSubNode(Character c, TrieNode node) {

            subNodes.put(c, node);

        }

        //获取子节点
        public TrieNode getSubNode(Character c) {
            return subNodes.get(c);
        }

    }

}
