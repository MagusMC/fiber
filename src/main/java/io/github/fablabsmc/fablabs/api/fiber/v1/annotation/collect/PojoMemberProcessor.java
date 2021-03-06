package io.github.fablabsmc.fablabs.api.fiber.v1.annotation.collect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.fablabsmc.fablabs.api.fiber.v1.exception.ProcessingMemberException;

/**
 * Implementors consume and process collected POJO members.
 */
public interface PojoMemberProcessor {
	void processListenerMethod(Object pojo, Method method, String name);

	void processListenerField(Object pojo, Field field, String name);

	void processSetting(Object pojo, Field setting) throws ProcessingMemberException;

	void processGroup(Object pojo, Field group) throws ProcessingMemberException;
}
