package com.mon4h.dashboard.common.config.impl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyConfigureFileReader extends
		ConfigureFileReaderBase<PropertyConfigureFile> {
	private static final Logger LOG = LoggerFactory
			.getLogger(PropertyConfigureFileReader.class);

	/**
	 * @param configFileS
	 */
	public PropertyConfigureFileReader(PropertyConfigureFile configFile) {
		super(configFile);
	}

	@Override
	public void load() throws Exception {
		FileReader fr = new FileReader(_configFile.getFileName());
		try {
			BufferedReader br = new BufferedReader(fr);
			while (br.ready()) {
				String txtLine = br.readLine();// read a line.
				String[] kvs = parseTextLine(txtLine);
				_configFile.setPropertyValue(kvs[0].trim(), kvs[1].trim());
			}

			br.close();
		} catch (IOException e) {
			logError(e.getMessage());
			// fr.close();
			throw new Exception(String.format(
					"An error occurred Loading config file %1sc.",
					_configFile.getFileName()));
		}

		super.load();
	}

	protected String[] parseTextLine(String txtLine) {
		String[] kvs = txtLine.split("=");
		if (kvs.length == 0) {
			// throw new
		}
		
		return kvs;
	}

	private static void logError(String err) {
		LOG.debug(err);
		LOG.error(err);
	}
}
