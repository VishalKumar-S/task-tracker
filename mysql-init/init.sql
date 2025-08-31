CREATE DATABASE IF NOT EXISTS notificationdb;

CREATE USER 'notifyUser'@'%' IDENTIFIED BY 'notifyPassword';

GRANT ALL PRIVILEGES ON notificationdb.* TO 'notifyUser'@'%';

FLUSH PRIVILEGES;