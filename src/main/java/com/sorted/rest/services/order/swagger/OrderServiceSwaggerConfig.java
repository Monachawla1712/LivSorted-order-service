/*
 * @author Mohit
 */
package com.sorted.rest.services.order.swagger;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sorted.rest.common.constants.ApplicationProfiles;
import com.sorted.rest.common.utils.ContextUtils;
import com.sorted.rest.services.common.swagger.SwaggerApiInfo;
import com.sorted.rest.services.common.swagger.SwaggerConfig;

/**
 * The Class CatalogSwaggerConfig.
 *
 * @author Mohit
 * @version $Id: $Id
 */
@Component
@Profile(value = { ApplicationProfiles.LOCAL, ApplicationProfiles.DEVELOPMENT, ApplicationProfiles.TEST })
public class OrderServiceSwaggerConfig extends SwaggerConfig {

	@Override
	public SwaggerApiInfo getApiInfo() {
		return getSwaggerFileInfo(ContextUtils.getBootName());
	}
}
