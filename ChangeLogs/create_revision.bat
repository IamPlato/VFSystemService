@ECHO OFF
SET FILENAME=revision.txt
REM 将Fix-nnnnnnnn.txt的内容合并为%FILENAME%的脚本
REM 将每个分支修改内容写入Fix-nnnnnnnn(使用年月日8位数字).txt
REM 这样可以避免多个分支在合并时%FILENAME%内容的冲突

dir Fix-*.txt /B /O-N > Fixlist.txt

REM 获取git commit id放入revision.txt中
for /f %%i in ('git rev-parse HEAD') do set COMMIT_ID=%%i
IF "%COMMIT_ID%"=="" (SET GIT_ID= ) ELSE (SET GIT_ID=GIT_COMMIT[%COMMIT_ID:~0,8%])

ECHO Created on %DATE:~0,10% %2 %GIT_ID% 1> %FILENAME% 

FOR /F %%i in (Fixlist.txt) do COPY %FILENAME%+%%i %FILENAME% /B && echo ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- >> %FILENAME% 
del Fixlist.txt
@IF "%1" NEQ "nopause" @PAUSE
