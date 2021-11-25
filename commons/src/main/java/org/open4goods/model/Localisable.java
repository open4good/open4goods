
package org.open4goods.model;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class Localisable extends HashMap<String, String> {

    private static final Logger logger = LoggerFactory.getLogger(Localisable.class);

    private static final long serialVersionUID = 7154423192084742663L;

    public String i18n(final HttpServletRequest request) {
        final String language = request.getLocale().getLanguage();
        if (null == language) {
            return get("default");
        }
        return this.getOrDefault(language, get("default"));
    }

    public String i18n(final String language) {
        if (null == language) {
            return i18n("default");
        }
        return this.getOrDefault(language, get("default"));
    }

    
    /**
//    *
//    * @param language
//    * @param param
//    * @return
//    */
//    public String i18n(final String language, final Object param) {
//        if (null == language) {
//            return i18n("default", param);
//        }
//
//        // Checking in uidMap
//        final String cacheKey = language + param.hashCode();
//        String val = i18nCache.get(cacheKey);
//
//        if (null == val) {
//
//            try {
//
//                final Context ctx = new Context();
//                ctx.setVariable("p", param);
//
//                final String tpl = i18n(language).replace("!{", "[[${").replaceAll("}", "}]]");
//                final String ret = EvaluationService.getTemplateEngine().process(tpl, ctx);
//                val = ret;
//                i18nCache.put(cacheKey, val);
//
//            } catch (final Exception e) {
//                logger.warn("Cannot generate template content", e);
//                val = "ERROR";
//            }
//        }
//
//        return val;
//    }

}
