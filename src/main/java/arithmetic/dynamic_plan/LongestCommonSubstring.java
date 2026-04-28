package arithmetic.dynamic_plan;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 最长公共子串
 * 给定两个字符串str1和str2,输出两个字符串的最长公共子串
 * 题目保证str1和str2的最长公共子串存在且唯一。
 */
public class LongestCommonSubstring {

    public static void main(String[] args) {
        System.out.println("NY92514w8AF5q1sul7MVNFZn".length());
        String str1 = "12AB2345CD";
        String str2 = "12345EF";
//        String str1 = "22222";
//        String str2 = "22222";
        //String str1 = "d8Wt20lnSgAw0HgauN2Kspyr298H6wQWMO3tMNRpWmR25NNTD4VTnq16LX80khSMEG0W5V72cIDLvy0WB1Nfnz4z51qrGNKT3xImT141NY92514w8AF5q1sul7MVNFZnGengc03vO912lFftHDkWpMwWN0SY4pXO1QLji18ujkZV4vr449Wo495WOyIXiO4C9M5L7hQ4tX9ePvV5ohnX00e4mOW28xO968cdR266Ej5M";
        // String str2 = "MV3Et2Q4x4YFlN304p5oLJzVT5zdfz8X83srj64mAx18Ai8kE82aF4so17uR3tD7Nch9CO775WHeVD166zgogKQAj4y04EjJ6Mc23Uvmt11NY92514w8AF5q1sul7MVNFZndJq1vh7qx45XOwP1k1M9jsbB3MLc9FFoy825lu0Cs9Bh3Xm84p5C36r6USQrF96W0b05RfF308001LpK89056qQ8517YFj4pM";
        System.out.println(lcs(str1, str2));
    }

     public String lcs1(String str1, String str2) {
        if (str1 == null || str2 == null || str1.length() == 0 || str2.length() == 0) {
            return "";
        }

        int m = str1.length();
        int n = str2.length();

        int[][] dp = new int[m + 1][n + 1];

        int maxLen = 0;
        int endIndex = 0; // 记录最长公共子串在 str1 中的结束位置

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;

                    if (dp[i][j] > maxLen) {
                        maxLen = dp[i][j];
                        endIndex = i;
                    }
                } else {
                    dp[i][j] = 0;
                }
            }
        }

        return str1.substring(endIndex - maxLen, endIndex);
    }

   public String lcs2(String str1, String str2) {
        if (str1 == null || str2 == null || str1.length() == 0 || str2.length() == 0) {
            return "";
        }

        int m = str1.length();
        int n = str2.length();

        int[] dp = new int[n + 1];

        int maxLen = 0;
        int endIndex = 0;

        for (int i = 1; i <= m; i++) {
            // 必须倒序，防止 dp[j - 1] 被当前行覆盖
            for (int j = n; j >= 1; j--) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[j] = dp[j - 1] + 1;

                    if (dp[j] > maxLen) {
                        maxLen = dp[j];
                        endIndex = i;
                    }
                } else {
                    dp[j] = 0;
                }
            }
        }

        return str1.substring(endIndex - maxLen, endIndex);
    }

    public static String lcs3(String str1, String str2) {
        // write code here
        int start = 0;
        int end = 1;
        String result = "";
        while (end <= str2.length()) {
            String tmp = str2.substring(start, end);
            if (str1.contains(tmp)) {
                result = tmp;
                end++;
                continue;
            }
            start++;
            end++;
        }
        return result;
    }


}
