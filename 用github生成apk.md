《用github生成apk》

注意：
.github 文件夹是手动创建的


# 更新题库并重新生成 APK 操作说明

## 一、题库文件位置

app\src\main\assets\questions.json


## 二、操作步骤

### 1. 修改题库
用记事本或其他文本编辑器打开 `questions.json`，按原有格式修改题目内容，保存。

### 2. 提交并推送到 GitHub
打开 **cmd**，执行：

```cmd
cd /d F:\svn\apk
git add .
git commit -m "update: 更新题库"
git push
```


### 3. 等待云端自动构建
推送后，GitHub Actions 会**自动**开始构建 APK（约 3-5 分钟）。

打开下面网址查看构建状态：

https://github.com/ws8638/apk/actions


### 4. 下载 APK
- 构建成功（绿色对勾）后，点进这次运行记录
- 拉到页面最底部 **Artifacts** 区域
- 点击 `app-debug-apk` 下载 zip
- 解压得到 `app-debug.apk`，传到手机安装即可

## 三、常见问题

### 构建失败（红色叉号）
点进失败的运行记录，查看哪个步骤报错，把报错截图或日志发给开发者排查。

### 推送失败
- 如果提示网络错误，多试几次
- 如果提示鉴权失败，确认 SSH 密钥已配置好

## 四、注意事项
- 修改 `questions.json` 时，确保 JSON 格式正确（括号、引号、逗号不能少）
- 每次修改后必须执行 `git add .` → `git commit` → `git push` 三步，云端才会重新构建
- APK 文件名固定为 `app-debug.apk`，无需签名即可安装到手机测试

