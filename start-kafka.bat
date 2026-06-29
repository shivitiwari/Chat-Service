@echo off
REM ================================================================
REM  Kafka 4.2.0 Startup Script for Windows (KRaft mode - no ZooKeeper)
REM ================================================================

SET KAFKA_HOME=C:\kafka\kafka_2.13-4.2.0

echo.
echo ========================================
echo   Starting Kafka 4.2.0 in KRaft mode
echo   (No ZooKeeper required!)
echo ========================================
echo.

REM Step 1: Generate a cluster UUID (only needed first time)
echo [1/3] Generating Cluster ID...
FOR /F "tokens=*" %%i IN ('%KAFKA_HOME%\bin\windows\kafka-storage.bat random-uuid') DO SET KAFKA_CLUSTER_ID=%%i
echo       Cluster ID: %KAFKA_CLUSTER_ID%

REM Step 2: Format the storage directory (only needed first time, safe to re-run)
echo [2/3] Formatting storage...
%KAFKA_HOME%\bin\windows\kafka-storage.bat format -t %KAFKA_CLUSTER_ID% -c %KAFKA_HOME%\config\server.properties --ignore-formatted

REM Step 3: Start Kafka
echo [3/3] Starting Kafka broker on localhost:9092 ...
echo.
echo       Press Ctrl+C to stop.
echo.
%KAFKA_HOME%\bin\windows\kafka-server-start.bat %KAFKA_HOME%\config\server.properties

