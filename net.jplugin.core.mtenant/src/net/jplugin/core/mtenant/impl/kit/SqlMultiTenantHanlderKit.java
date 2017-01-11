package net.jplugin.core.mtenant.impl.kit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import net.jplugin.core.config.api.ConfigFactory;
import net.jplugin.core.kernel.api.ctx.ThreadLocalContextManager;
import net.jplugin.core.mtenant.impl.kit.parser.SqlParser;
import net.jplugin.core.mtenant.impl.kit.parser.impl.DeleteSqlParser;
import net.jplugin.core.mtenant.impl.kit.parser.impl.InsertSqlParser;
import net.jplugin.core.mtenant.impl.kit.parser.impl.SelectSqlParser;
import net.jplugin.core.mtenant.impl.kit.parser.impl.UpdateSqlParser;
import net.jplugin.core.mtenant.impl.kit.utils.SqlHelper;
import net.jplugin.core.mtenant.impl.kit.utils.StringUtils;

public class SqlMultiTenantHanlderKit {
	/**
	 * @param dataSourceName
	 * @param sql
	 * @return
	 */
	private static ConcurrentHashMap<String, Object> params = null;
	private static ConcurrentHashMap<String, List<String>> ignores = null;

	public static String handle(String dataSourceName, String sql) {
		if (ConfigFactory.getStringConfig("mtenant.enable") == null || "false".equalsIgnoreCase(ConfigFactory.getStringConfig("mtenant.enable"))) {
			return sql;
		}

		sql = SqlHelper.format(sql);

		if (StringUtils.contains(sql, "ignore-tenant")) {
			return sql;
		}

		if (params == null) {
			params = new ConcurrentHashMap<>();
			params.put(ConfigFactory.getStringConfig("mtenant.field"), ThreadLocalContextManager.getRequestInfo().getCurrentTenantId());
		}

		String datasource = ConfigFactory.getStringConfig("mtenant.datasource");
		if (ignores == null) {
			ignores = new ConcurrentHashMap<>();
			if (!"ALL".equalsIgnoreCase(datasource)) {
				String[] datasources = StringUtils.split(datasource.trim(), ",");
				for (String s : datasources) {
					String exclude = ConfigFactory.getStringConfig("mtenant.datasource." + s + ".exclude");
					if (exclude != null) {
						String[] excludes = StringUtils.split(exclude, ",");
						List<String> list = new ArrayList<>();
						for (String t : excludes) {
							list.add(t);
						}
						ignores.put(s, list);
					}
				}
			} else {
				String exclude = ConfigFactory.getStringConfig("mtenant.datasource." + dataSourceName + ".exclude");
				if (exclude != null) {
					String[] excludes = StringUtils.split(exclude, ",");
					List<String> list = new ArrayList<>();
					for (String t : excludes) {
						list.add(t);
					}
					ignores.put(dataSourceName, list);
				}
			}
		} else {
			if ("ALL".equalsIgnoreCase(datasource) || StringUtils.contains(datasource, dataSourceName)) {
				if (!ignores.containsKey(dataSourceName)) {
					String exclude = ConfigFactory.getStringConfig("mtenant.datasource." + dataSourceName + ".exclude");
					if (exclude != null) {
						String[] excludes = StringUtils.split(exclude, ",");
						List<String> list = new ArrayList<>();
						for (String t : excludes) {
							list.add(t);
						}
						ignores.put(dataSourceName, list);
					}
				}
			}
		}

		SqlParser parser;
		if (StringUtils.startsWithIgnoreCase(sql, "select")) {
			parser = new SelectSqlParser();
		} else if (StringUtils.startsWithIgnoreCase(sql, "update")) {
			parser = new UpdateSqlParser();
		} else if (StringUtils.startsWithIgnoreCase(sql, "insert")) {
			parser = new InsertSqlParser();
		} else if (StringUtils.startsWithIgnoreCase(sql, "delete")) {
			parser = new DeleteSqlParser();
		} else {
			throw new RuntimeException("not support this sql to parse : " + sql);
		}
		return parser.parse(sql, params, ignores.get(dataSourceName));
	}

}