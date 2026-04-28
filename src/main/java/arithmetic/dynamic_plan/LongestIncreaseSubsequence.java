package arithmetic.dynamic_plan;

import java.util.Arrays;

/**
 * 最长上升子序列一
 * 给定一个长度为 n 的数组 arr，求它的最长严格上升子序列的长度。
 * 所谓子序列，指一个数组删掉一些数（也可以不删）之后，形成的新数组。例如 [1,5,3,7,3] 数组，其子序列有：[1,3,3]、[7] 等。但 [1,6]、[1,3,5] 则不是它的子序列。
 */
public class LongestIncreaseSubsequence {
    public static void main(String[] args) {
        int[] array = {6, 3, 1, 5, 2, 3, 7};
        System.out.println(lis(array));
    }

    public int LIS(int[] arr) {
        if (arr == null || arr.length == 0) {
            return 0;
        }

        int n = arr.length;
        int[] dp = new int[n];

        int max = 1;

        for (int i = 0; i < n; i++) {
            dp[i] = 1;

            for (int j = 0; j < i; j++) {
                if (arr[j] < arr[i]) {
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }

            max = Math.max(max, dp[i]);
        }

        return max;
    }

     public int LIS(int[] arr) {
        if (arr == null || arr.length == 0) {
            return 0;
        }

        int[] tails = new int[arr.length];
        int size = 0;

        for (int num : arr) {
            int left = 0;
            int right = size;

            // 找第一个 >= num 的位置
            while (left < right) {
                int mid = left + (right - left) / 2;

                if (tails[mid] < num) {
                    left = mid + 1;
                } else {
                    right = mid;
                }
            }

            tails[left] = num;

            if (left == size) {
                size++;
            }
        }

        return size;
    }


    public int lcs3(int[] arr) {
        // write code here
        if (arr == null || arr.length == 0) {
            return 0;
        }
        int len = arr.length;
        int maxLen = 0;
        int[] dp = new int[len];
        Arrays.fill(dp, 1);
        for (int i = 1; i < len; i++) {
            for (int j = i - 1; j >= 0; j--) {
                if (arr[i] > arr[j]) {
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }
            maxLen = Math.max(maxLen, dp[i]);
        }
        return maxLen;
    }
}
