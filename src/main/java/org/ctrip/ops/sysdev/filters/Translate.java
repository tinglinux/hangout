package org.ctrip.ops.sysdev.filters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;

import scala.Tuple2;

import org.apache.log4j.Logger;
import org.ctrip.ops.sysdev.render.FreeMarkerRender;
import org.ctrip.ops.sysdev.render.TemplateRender;
import org.yaml.snakeyaml.Yaml;

public class Translate extends BaseFilter {
	private static final Logger logger = Logger.getLogger(Translate.class
			.getName());

	public Translate(Map config) {
		super(config);
	}

	private String target;
	private String source;
	private String dictionaryPath;
	private int refreshInterval;
	private long nextLoadTime;
	private HashMap dictionary;

	private void loadDictionary() {
		if (dictionaryPath == null) {
			dictionary = null;
			logger.warn("dictionary_path is null");
			return;
		}
		Yaml yaml = new Yaml();
		FileInputStream input;
		try {
			input = new FileInputStream(new File(dictionaryPath));
			dictionary = (HashMap) yaml.load(input);
		} catch (FileNotFoundException e) {
			logger.error(dictionaryPath + " is not found");
			logger.error(e.getMessage());
			dictionary = null;
		}
	}

	protected void prepare() {
		target = (String) config.get("target");
		source = (String) config.get("source");

		dictionaryPath = (String) config.get("dictionary_path");

		loadDictionary();

		if (config.containsKey("refresh_interval")) {
			this.refreshInterval = (int) config.get("refresh_interval") * 1000;
		} else {
			this.refreshInterval = 300 * 1000;
		}
		nextLoadTime = System.currentTimeMillis() + refreshInterval * 1000;
	};

	@Override
	protected Map filter(final Map event) {
		if (dictionary == null || !event.containsKey(this.source)) {
			return event;
		}
		if (System.currentTimeMillis() >= nextLoadTime) {
			loadDictionary();
			nextLoadTime += refreshInterval;
		}
		Object t = dictionary.get(event.get(source));
		if (t != null) {
			event.put(target, t);
		}
		return event;
	}
}
