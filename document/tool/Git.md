# Git 常用命令总结

Git 常用命令主要分为：

```text
初始化
配置
查看状态
提交代码
分支管理
远程仓库
合并代码
撤销回退
暂存代码
日志查看
标签管理
```

---

## 1. 初始化仓库

```bash
git init
```

作用：

```text
在当前目录初始化一个 Git 仓库
```

克隆远程仓库：

```bash
git clone 仓库地址
```

示例：

```bash
git clone https://github.com/demo/project.git
```

---

## 2. Git 配置

查看配置：

```bash
git config --list
```

配置用户名：

```bash
git config --global user.name "your name"
```

配置邮箱：

```bash
git config --global user.email "your email"
```

查看当前用户名：

```bash
git config user.name
```

查看当前邮箱：

```bash
git config user.email
```

---

## 3. 查看状态

```bash
git status
```

作用：

```text
查看当前工作区、暂存区状态
```

查看简洁状态：

```bash
git status -s
```

---

## 4. 添加到暂存区

添加指定文件：

```bash
git add 文件名
```

添加所有文件：

```bash
git add .
```

添加所有修改和删除：

```bash
git add -A
```

---

## 5. 提交代码

```bash
git commit -m "提交说明"
```

示例：

```bash
git commit -m "fix: 修复登录异常"
```

添加并提交已跟踪文件：

```bash
git commit -am "提交说明"
```

注意：

```text
git commit -am 不会提交新文件，只会提交已经被 Git 跟踪过的文件
```

---

## 6. 查看提交日志

普通日志：

```bash
git log
```

简洁日志：

```bash
git log --oneline
```

查看图形日志：

```bash
git log --oneline --graph --all
```

查看某个文件日志：

```bash
git log 文件名
```

---

## 7. 查看差异

查看工作区和暂存区差异：

```bash
git diff
```

查看暂存区和最后一次提交差异：

```bash
git diff --cached
```

查看两个提交之间差异：

```bash
git diff commitId1 commitId2
```

---

## 8. 分支管理

查看本地分支：

```bash
git branch
```

查看所有分支：

```bash
git branch -a
```

创建分支：

```bash
git branch 分支名
```

切换分支：

```bash
git checkout 分支名
```

创建并切换分支：

```bash
git checkout -b 分支名
```

新版本推荐：

```bash
git switch 分支名
```

创建并切换：

```bash
git switch -c 分支名
```

删除本地分支：

```bash
git branch -d 分支名
```

强制删除本地分支：

```bash
git branch -D 分支名
```

---

## 9. 远程仓库

查看远程仓库：

```bash
git remote -v
```

添加远程仓库：

```bash
git remote add origin 仓库地址
```

修改远程仓库地址：

```bash
git remote set-url origin 仓库地址
```

删除远程仓库：

```bash
git remote remove origin
```

---

## 10. 拉取代码

拉取远程代码并合并：

```bash
git pull
```

拉取指定分支：

```bash
git pull origin 分支名
```

只拉取远程更新，不自动合并：

```bash
git fetch
```

拉取远程所有分支信息：

```bash
git fetch --all
```

---

## 11. 推送代码

推送当前分支：

```bash
git push
```

推送到指定远程分支：

```bash
git push origin 分支名
```

第一次推送并建立关联：

```bash
git push -u origin 分支名
```

删除远程分支：

```bash
git push origin --delete 分支名
```

---

## 12. 合并代码

合并指定分支到当前分支：

```bash
git merge 分支名
```

示例：

```bash
git checkout master
git merge dev
```

变基：

```bash
git rebase 分支名
```

简单理解：

```text
merge：保留分支合并记录
rebase：让提交历史更线性
```

---

## 13. 解决冲突

冲突后查看状态：

```bash
git status
```

解决冲突后：

```bash
git add .
git commit -m "resolve conflict"
```

如果是 rebase 冲突：

```bash
git add .
git rebase --continue
```

放弃 rebase：

```bash
git rebase --abort
```

---

## 14. 撤销修改

撤销工作区文件修改：

```bash
git checkout -- 文件名
```

新版本推荐：

```bash
git restore 文件名
```

撤销暂存区文件：

```bash
git reset HEAD 文件名
```

新版本推荐：

```bash
git restore --staged 文件名
```

---

## 15. 回退版本

回退到上一个版本，保留代码修改：

```bash
git reset --soft HEAD~1
```

回退到上一个版本，保留工作区修改，但取消暂存：

```bash
git reset --mixed HEAD~1
```

回退到上一个版本，代码也回退：

```bash
git reset --hard HEAD~1
```

回退到指定提交：

```bash
git reset --hard commitId
```

注意：

```text
reset --hard 会丢失本地修改，慎用
```

---

## 16. 安全回滚提交

生成一个新的反向提交：

```bash
git revert commitId
```

适合：

```text
已经推送到远程的提交回滚
```

和 `reset` 区别：

```text
reset 会改提交历史
revert 不改历史，只新增一次回滚提交
```

---

## 17. 暂存代码

暂存当前修改：

```bash
git stash
```

暂存并添加说明：

```bash
git stash save "说明"
```

查看暂存列表：

```bash
git stash list
```

恢复最近一次暂存：

```bash
git stash pop
```

恢复但不删除暂存记录：

```bash
git stash apply
```

删除某个暂存：

```bash
git stash drop stash@{0}
```

清空暂存：

```bash
git stash clear
```

---

## 18. 标签管理

查看标签：

```bash
git tag
```

创建标签：

```bash
git tag v1.0.0
```

给指定提交打标签：

```bash
git tag v1.0.0 commitId
```

推送标签：

```bash
git push origin v1.0.0
```

推送所有标签：

```bash
git push origin --tags
```

删除本地标签：

```bash
git tag -d v1.0.0
```

删除远程标签：

```bash
git push origin --delete tag v1.0.0
```

---

## 19. 常用组合命令

### 提交并推送

```bash
git add .
git commit -m "提交说明"
git push
```

---

### 拉取最新代码

```bash
git pull origin 分支名
```

---

### 创建新分支并推送

```bash
git checkout -b feature/login
git push -u origin feature/login
```

---

### 回退最近一次提交但保留代码

```bash
git reset --soft HEAD~1
```

---

### 丢弃所有本地修改

```bash
git reset --hard
git clean -fd
```

注意：

```text
会删除未提交修改和未跟踪文件，慎用
```

---

## 20. 总结

Git 常用命令主要包括：`git init` 初始化仓库，`git clone` 克隆项目，`git status` 查看状态，`git add` 添加暂存，`git commit` 提交代码，`git pull` 拉取远程代码，`git push` 推送代码，`git branch` 管理分支，`git merge` 合并分支，`git rebase` 变基，`git reset` 回退版本，`git revert` 安全回滚，`git stash` 暂存代码。

一句话总结：

```text
Git 常用流程 = pull 拉代码 + branch 建分支 + add 暂存 + commit 提交 + push 推送 + merge/rebase 合并。
```
