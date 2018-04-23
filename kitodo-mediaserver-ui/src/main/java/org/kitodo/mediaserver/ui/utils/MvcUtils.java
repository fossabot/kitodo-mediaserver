/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * LICENSE file that was distributed with this source code.
 */

package org.kitodo.mediaserver.ui.utils;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;


/**
 * Helpers for MVC actions.
 */
public class MvcUtils {

    /**
     * Check for the error variable and set the error message to the model if available.
     * @param model MVC model
     * @param error error code
     * @param locale language settings
     * @param messageSource messagesource
     */
    public static void handleRedirectError(Model model, @ModelAttribute("error") String error, Locale locale, MessageSource messageSource) {
        if (StringUtils.hasText(error)) {
            try {
                String msg = messageSource.getMessage(error, new Object[]{}, locale);
                model.addAttribute("error", msg);
            } catch (NoSuchMessageException e) {
                model.addAttribute("error", error);
            }
        }
    }
}
