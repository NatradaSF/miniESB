package sf.sfis.miniesb.utility;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBElement;

public class JAXBHelper {
	@SuppressWarnings("unchecked")
	public static <T> List<T> extractElementsByLocalPart(Object container, String expectedLocalPart,
			Class<T> expectedType) {
		List<T> result = new ArrayList<>();

		try {
			Method getContentMethod = container.getClass().getMethod("getContent");
			Object contentList = getContentMethod.invoke(container);

			if (contentList instanceof List<?>) {
				for (Object item : (List<?>) contentList) {
					if (item instanceof JAXBElement) {
						JAXBElement<?> el = (JAXBElement<?>) item;
						if (expectedLocalPart.equals(el.getName().getLocalPart())) {
							Object value = el.getValue();
							if (expectedType.isInstance(value)) {
								result.add((T) value);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	public static <T> T extractElementByLocalPart(Object container, String expectedLocalPart, Class<T> expectedType) {
		try {
			Method getContentMethod = container.getClass().getMethod("getContent");
			Object contentList = getContentMethod.invoke(container);

			if (contentList instanceof List<?>) {
				for (Object item : (List<?>) contentList) {
					if (item instanceof JAXBElement) {
						JAXBElement<?> el = (JAXBElement<?>) item;
						if (expectedLocalPart.equals(el.getName().getLocalPart())) {
							Object value = el.getValue();
							if (expectedType.isInstance(value)) {
								return (T) value;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
