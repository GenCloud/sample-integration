package org.genfork.integration.model.components.result;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.genfork.integration.model.components.AbstractDefinition;
import org.genfork.integration.stream.annotation.SettingClass;
import org.genfork.integration.stream.annotation.SettingValue;
import org.genfork.integration.stream.annotation.StreamComponent;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.MessageChannel;

import static org.genfork.integration.stream.annotation.StreamComponent.ComponentType.app;

/**
 * Model storage parameters for populating the provided {@link MessageChannel} instance
 * at the current {@link IntegrationFlow} chain position.
 *
 * @author: GenCloud
 * @date: 2020/07
 */
@StreamComponent(componentName = "result", componentType = app)
@SettingClass
@Data
@EqualsAndHashCode(callSuper = true)
public class ResultDefinition extends AbstractDefinition<ResultDefinition> {
	@SettingValue(originName = "result-channel", description = "Наименование диспетчера исходящего канала", required = true)
	private String replyChannel;

	@SettingValue(description = "Булевый оператор, определяющий нужно ли вернуть результат или нет (по умолчанию - 'false')")
	private boolean cancel;

	@SettingValue(originName = "result-to-null", description = "Отправка результата в 'нулевой' канал", required = true)
	private boolean resultToNull;

	@SettingValue(originName = "search-in-global-context", description = "Признак поиска канала в глобальном контексте")
	private boolean searchInGlobalContext;

	@Override
	public boolean isResultDefinition() {
		return true;
	}
}
