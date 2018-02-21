package com.mantledillusion.vaadin.cotton;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

final class LocalizationControl extends java.util.ResourceBundle.Control {

	private final Charset charset;
	private final String extension;

	public LocalizationControl(Charset charset, String extension) {
		this.charset = charset;
		this.extension = extension;
	}

	@Override
	public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader,
			boolean reload) throws IllegalAccessException, InstantiationException, IOException {

		final String bundleName = toBundleName(baseName, locale);
		final String resourceName = toResourceName(bundleName, this.extension);
		ResourceBundle bundle = null;
		InputStream stream = null;
		if (reload) {
			final URL url = loader.getResource(resourceName);
			if (url != null) {
				final URLConnection connection = url.openConnection();
				if (connection != null) {
					connection.setUseCaches(false);
					stream = connection.getInputStream();
				}
			}
		} else {
			stream = loader.getResourceAsStream(resourceName);
		}
		if (stream != null) {
			try {
				bundle = new PropertyResourceBundle(new InputStreamReader(stream, this.charset));
			} finally {
				stream.close();
			}
		}
		return bundle;
	}

	@Override
	public String toBundleName(String baseName, Locale locale) {
		return baseName + '_' + locale.getISO3Language();
	}

	@Override
	public Locale getFallbackLocale(String baseName, Locale locale) {
		return null;
	}
}