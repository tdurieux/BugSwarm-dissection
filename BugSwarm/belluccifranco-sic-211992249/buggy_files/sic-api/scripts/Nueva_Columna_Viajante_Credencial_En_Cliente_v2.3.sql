ALTER TABLE `sic`.`cliente` 
ADD COLUMN `id_Usuario_Credencial` BIGINT(20) NULL DEFAULT NULL AFTER `id_Localidad`,
ADD COLUMN `id_Usuario_Viajante` BIGINT(20) NULL DEFAULT NULL AFTER `id_Usuario_Credencial`;
