@echo off
echo Running JDEConnectorJavaUtil ..
java -jar JDEConnectorJavaUtil.jar -u JDE -p Modus2017! -e JDV920 -r *ALL -c 4 -s 10 -b ube_R0008P_XJDE0001.xml >run.log
echo           RUN Logs: runR0008P.log" 
pause "Press Enter to End"