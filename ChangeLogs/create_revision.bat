@ECHO OFF
SET FILENAME=revision.txt
REM ��Fix-nnnnnnnn.txt�����ݺϲ�Ϊ%FILENAME%�Ľű�
REM ��ÿ����֧�޸�����д��Fix-nnnnnnnn(ʹ��������8λ����).txt
REM �������Ա�������֧�ںϲ�ʱ%FILENAME%���ݵĳ�ͻ

dir Fix-*.txt /B /O-N > Fixlist.txt

REM ��ȡgit commit id����revision.txt��
for /f %%i in ('git rev-parse HEAD') do set COMMIT_ID=%%i
IF "%COMMIT_ID%"=="" (SET GIT_ID= ) ELSE (SET GIT_ID=GIT_COMMIT[%COMMIT_ID:~0,8%])

ECHO Created on %DATE:~0,10% %2 %GIT_ID% 1> %FILENAME% 

FOR /F %%i in (Fixlist.txt) do COPY %FILENAME%+%%i %FILENAME% /B && echo ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- >> %FILENAME% 
del Fixlist.txt
@IF "%1" NEQ "nopause" @PAUSE
