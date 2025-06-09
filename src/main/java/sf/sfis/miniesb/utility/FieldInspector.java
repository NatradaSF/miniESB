package sf.sfis.miniesb.utility;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sf.sfis.miniesb.controller.RedisController;

public class FieldInspector {
	private static final Logger LOGGER = LoggerFactory.getLogger(FieldInspector.class);
	
	public static List<String> getNonNullFields(Object obj) {
		List<String> nonNullFields = new ArrayList<>();

		for (Field field : obj.getClass().getDeclaredFields()) {
			field.setAccessible(true); // เข้าถึง private field
			try {
				Object value = field.get(obj);
				if (value != null && (value instanceof String || value instanceof BigDecimal)) {
					nonNullFields.add(field.getName());
				}
			} catch (IllegalAccessException e) {
				LOGGER.error("⚠️ ไม่สามารถเข้าถึง field: " + field.getName());
			}
		}

		return nonNullFields;
	}
	
	public static boolean allFieldsAreNull(Object obj) {
        if (obj == null) return true;

        for (Field field : obj.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true); // เปิดให้เข้าถึง private fields ได้
                Object value = field.get(obj);
                if (value != null && !value.toString().trim().equals("")) {
                    return false; // เจอ field ที่ไม่ null
                }
            } catch (IllegalAccessException e) {
				LOGGER.error("allFieldsAreNull: " + e);
                return false;
            }
        }
        return true;
    }
	
	public static void replaceHoldWithEmpty(Object obj) {
        if (obj == null) return;

        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            try {
                field.setAccessible(true);
                // ตรวจสอบเฉพาะ field ที่เป็น String
                if (field.getType() == String.class) {
                    Object value = field.get(obj);
                    if ("HOLD".equals(value)) {
                        field.set(obj, " ");
                    }
                }

            } catch (IllegalAccessException e) {
				LOGGER.error("Cannot access field: " + field.getName());
                // จะ continue ไป field ถัดไปแม้เจอปัญหา
            }
        }
    }
}
