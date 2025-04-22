/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 4/4/2025
 * Github link: https://github.com/kireiiiiiiii
 */

package flaggi.shared.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Pattern;

public class NetUtil {

	// Private constructor to prevent instantiation
	private NetUtil() {
		throw new UnsupportedOperationException("NetUtil is a utility class and cannot be instantiated.");
	}

	// Methods -------------------------------------------------------------------

	public static InetAddress getLocalIPv4Address() {
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			while (networkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = networkInterfaces.nextElement();

				if (networkInterface.isLoopback() || !networkInterface.isUp()) {
					continue;
				}

				Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
				while (inetAddresses.hasMoreElements()) {
					InetAddress inetAddress = inetAddresses.nextElement();
					if (inetAddress instanceof Inet4Address) {
						return inetAddress;
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean isValidAddress(String input) {
		String addressRegex = "^(?:(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}|localhost|\\d{1,3}(\\.\\d{1,3}){3}|\\[([a-fA-F0-9:]+)])";
		String portRegex = "(:([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5]))?$";
		String fullRegex = addressRegex + portRegex;
		return Pattern.compile(fullRegex).matcher(input).matches();
	}
}
