# 设置 JAVA_HOME 为实际的 JDK 安装路径
$env:JAVA_HOME = "D:\Java\jdk-23.0.2"
Write-Output "JAVA_HOME=$env:JAVA_HOME"
& "C:\Users\67161\ai-infra\nacos\standalone\nacos-3.2.2\bin\startup.cmd" -m standalone
