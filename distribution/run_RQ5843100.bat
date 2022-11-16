@echo off
echo Running JDEConnectorJavaUtil ..
java -jar JDEConnectorJavaUtil.jar -u JDE -p XXXXXX -e JDV920 -r *ALL -c 4 -s 60 -b ube_RQ5843100_LOAD01.xml >runRQ5843100.log
echo           RUN Logs: runRQ5843100.log" 
pause "Press Enter to End"