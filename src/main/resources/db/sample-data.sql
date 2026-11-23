-- Datos de ejemplo OPCIONALES (no se cargan automáticamente).
-- Para cargarlos al arrancar:
--   SPRING_SQL_INIT_DATA_LOCATIONS=classpath:db/sample-data.sql
-- MRC = mbps x 2.50 x (1 - descuento) ; 12m 0%, 24m 5%, 36m 10%

INSERT INTO oferta (cliente, estado, version) VALUES ('Cliente Demo S.A.', 'BORRADOR', 0);
INSERT INTO servicio (oferta_id, tipo, mbps, plazo_meses, viable, mrc)
VALUES ((SELECT MAX(id) FROM oferta), 'INTERNET', 100, 36, TRUE, 225.00);
INSERT INTO servicio (oferta_id, tipo, mbps, plazo_meses, viable, mrc)
VALUES ((SELECT MAX(id) FROM oferta), 'CAPACIDAD', 50, 24, TRUE, 118.75);

INSERT INTO oferta (cliente, estado, version) VALUES ('Operador Andino Ltda.', 'BORRADOR', 0);
INSERT INTO servicio (oferta_id, tipo, mbps, plazo_meses, viable, mrc)
VALUES ((SELECT MAX(id) FROM oferta), 'INTERNET', 200, 12, TRUE, 500.00);
INSERT INTO servicio (oferta_id, tipo, mbps, plazo_meses, viable, mrc)
VALUES ((SELECT MAX(id) FROM oferta), 'CAPACIDAD', 1000, 36, FALSE, 2250.00);
