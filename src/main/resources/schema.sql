CREATE OR REPLACE FUNCTION caros_tabla(vieja text, nueva text) RETURNS void AS $$
BEGIN
  IF to_regclass(vieja) IS NOT NULL AND to_regclass(nueva) IS NULL THEN
    EXECUTE format('ALTER TABLE %I RENAME TO %I', vieja, nueva);
  END IF;
END $$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION caros_col(tabla text, vieja text, nueva text) RETURNS void AS $$
BEGIN
  IF to_regclass(tabla) IS NOT NULL
     AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = tabla AND column_name = vieja)
     AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = tabla AND column_name = nueva)
  THEN
    EXECUTE format('ALTER TABLE %I RENAME COLUMN %I TO %I', tabla, vieja, nueva);
  END IF;
END $$ LANGUAGE plpgsql;

SELECT caros_tabla('vehicles', 'vehiculos');
SELECT caros_tabla('signals', 'senales');
SELECT caros_tabla('scan_readings', 'lecturas');
SELECT caros_tabla('work_orders', 'ordenes');
SELECT caros_tabla('part_preorders', 'preordenes');
SELECT caros_tabla('vehicle_updates', 'avances');
SELECT caros_tabla('update_media', 'archivos');
SELECT caros_tabla('parts', 'refacciones');
SELECT caros_tabla('suppliers', 'proveedores');
SELECT caros_tabla('supplier_quotes', 'cotizaciones');
SELECT caros_tabla('mail_messages', 'correos');
SELECT caros_tabla('chat_messages', 'chats');
SELECT caros_tabla('password_resets', 'recuperaciones');

SELECT caros_col('usuarios', 'email', 'correo');
SELECT caros_col('usuarios', 'password', 'contrasena');
SELECT caros_col('usuarios', 'display_name', 'nombre');
SELECT caros_col('usuarios', 'provider', 'origen');

SELECT caros_col('vehiculos', 'plate', 'placa');
SELECT caros_col('vehiculos', 'brand', 'marca');
SELECT caros_col('vehiculos', 'model', 'modelo');
SELECT caros_col('vehiculos', 'year', 'anio');
SELECT caros_col('vehiculos', 'health', 'salud');

SELECT caros_col('senales', 'vehicle_id', 'vehiculo_id');
SELECT caros_col('senales', 'code', 'codigo');
SELECT caros_col('senales', 'description', 'descripcion');
SELECT caros_col('senales', 'severity', 'gravedad');
SELECT caros_col('senales', 'suggestion', 'sugerencia');
SELECT caros_col('senales', 'status', 'estado');
SELECT caros_col('senales', 'created_at', 'creado_en');
SELECT caros_col('senales', 'closed_at', 'cerrado_en');

SELECT caros_col('lecturas', 'vehicle_id', 'vehiculo_id');
SELECT caros_col('lecturas', 'source', 'origen');
SELECT caros_col('lecturas', 'dtc_codes', 'codigos_dtc');
SELECT caros_col('lecturas', 'created_new', 'unidad_nueva');
SELECT caros_col('lecturas', 'created_at', 'creado_en');

SELECT caros_col('ordenes', 'vehicle_id', 'vehiculo_id');
SELECT caros_col('ordenes', 'work_desc', 'descripcion');
SELECT caros_col('ordenes', 'technician', 'tecnico');
SELECT caros_col('ordenes', 'scheduled_at', 'programada_en');
SELECT caros_col('ordenes', 'hours', 'horas');
SELECT caros_col('ordenes', 'status', 'estado');
SELECT caros_col('ordenes', 'part_name', 'nombre_refaccion');
SELECT caros_col('ordenes', 'supplier_name', 'nombre_proveedor');
SELECT caros_col('ordenes', 'lead_days', 'dias_entrega');

SELECT caros_col('preordenes', 'vehicle_id', 'vehiculo_id');
SELECT caros_col('preordenes', 'code', 'codigo');
SELECT caros_col('preordenes', 'part_name', 'nombre_refaccion');
SELECT caros_col('preordenes', 'supplier_name', 'nombre_proveedor');
SELECT caros_col('preordenes', 'price', 'precio');
SELECT caros_col('preordenes', 'lead_days', 'dias_entrega');
SELECT caros_col('preordenes', 'eta', 'llegada');
SELECT caros_col('preordenes', 'from_stock', 'de_almacen');
SELECT caros_col('preordenes', 'created_at', 'creado_en');

SELECT caros_col('avances', 'vehicle_id', 'vehiculo_id');
SELECT caros_col('avances', 'kind', 'tipo');
SELECT caros_col('avances', 'message', 'mensaje');
SELECT caros_col('avances', 'created_at', 'creado_en');

SELECT caros_col('archivos', 'update_id', 'avance_id');
SELECT caros_col('archivos', 'media_type', 'tipo_archivo');

SELECT caros_col('refacciones', 'name', 'nombre');
SELECT caros_col('refacciones', 'category', 'categoria');
SELECT caros_col('refacciones', 'price', 'precio');
SELECT caros_col('refacciones', 'stock', 'existencia');
SELECT caros_col('refacciones', 'compatibility', 'compatibilidad');

SELECT caros_col('proveedores', 'name', 'nombre');
SELECT caros_col('proveedores', 'city', 'ciudad');

SELECT caros_col('cotizaciones', 'part_id', 'refaccion_id');
SELECT caros_col('cotizaciones', 'supplier_id', 'proveedor_id');
SELECT caros_col('cotizaciones', 'price', 'precio');
SELECT caros_col('cotizaciones', 'lead_days', 'dias_entrega');

SELECT caros_col('correos', 'ref_key', 'clave');
SELECT caros_col('correos', 'to_email', 'correo_destino');
SELECT caros_col('correos', 'to_name', 'nombre_destino');
SELECT caros_col('correos', 'subject', 'asunto');
SELECT caros_col('correos', 'body', 'cuerpo');
SELECT caros_col('correos', 'kind', 'tipo');
SELECT caros_col('correos', 'plate', 'placa');
SELECT caros_col('correos', 'created_at', 'creado_en');

SELECT caros_col('chats', 'role', 'emisor');
SELECT caros_col('chats', 'text', 'texto');
SELECT caros_col('chats', 'created_at', 'creado_en');

SELECT caros_col('recuperaciones', 'email', 'correo');
SELECT caros_col('recuperaciones', 'expires_at', 'expira_en');
SELECT caros_col('recuperaciones', 'used', 'usado');

DROP FUNCTION caros_tabla(text, text);
DROP FUNCTION caros_col(text, text);
