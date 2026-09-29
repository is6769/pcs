BEGIN;
INSERT INTO clients (name, email, company) VALUES
('Анна Иванова', 'anna@example.com', 'Альфа'),
('Борис Петров', 'boris@example.com', 'Бета'),
('Вера Смирнова', 'vera@example.com', 'Гамма'),
('Дмитрий Орлов', 'dmitry@example.com', 'Дельта'),
('Елена Соколова', 'elena@example.com', 'Омега');
INSERT INTO resource_requests (client_id,title,description,resource_type,region,cpu,ram_gb,storage_gb,status,created_at) VALUES
(1,'Веб-сервер магазина','Публичный каталог товаров','VIRTUAL_MACHINE','RU_MOSCOW',4,8,100,'NEW','2026-09-01 10:00'),
(2,'База аналитики','PostgreSQL для отчетов','DATABASE','RU_SPB',8,32,500,'APPROVED','2026-09-02 11:00'),
(3,'Архив документов','Долговременное хранение файлов','OBJECT_STORAGE','RU_KAZAN',0,0,2000,'PROVISIONED','2026-09-03 12:00'),
(4,'Тестовый стенд','Проверка новой версии API','VIRTUAL_MACHINE','RU_MOSCOW',2,4,50,'CANCELLED','2026-09-04 09:00'),
(5,'База заказов','Основная PostgreSQL база магазина','DATABASE','RU_MOSCOW',16,64,1000,'PROVISIONED','2026-09-05 14:00'),
(1,'Резервные копии','Ежедневные архивы базы','OBJECT_STORAGE','RU_SPB',0,0,5000,'NEW','2026-09-06 10:00'),
(2,'Сервер обработки','Пакетная обработка отчетов','VIRTUAL_MACHINE','RU_KAZAN',32,128,2000,'REJECTED','2026-09-07 15:00'),
(3,'База разработки','Изолированная среда разработки','DATABASE','RU_SPB',2,8,100,'NEW','2026-09-08 16:00'),
(4,'Медиафайлы','Изображения и видео','OBJECT_STORAGE','RU_MOSCOW',0,0,3000,'APPROVED','2026-09-09 17:00'),
(5,'Сервер мониторинга','Метрики облачной инфраструктуры','VIRTUAL_MACHINE','RU_KAZAN',4,16,200,'PROVISIONED','2026-09-10 18:00');
COMMIT;
