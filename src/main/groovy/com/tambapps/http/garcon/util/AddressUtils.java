package com.tambapps.http.garcon.util;

import lombok.SneakyThrows;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * util methods for IP addresses. Will soon be removed
 */
@Deprecated
public class AddressUtils {

  /**
   * from <a href="https://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device-from-code">this link</a>
   *
   * @return return the ip address of the device
   * @throws IOException in case of I/O errors
   */
  public static List<InetAddress> getIpAddresses() throws IOException {
    ArrayList<NetworkInterface> interfaces =
        Collections.list(NetworkInterface.getNetworkInterfaces());
    return interfaces.stream()

        .flatMap(i -> Collections.list(i.getInetAddresses()).stream())
        .filter(addr -> !addr.isLoopbackAddress() &&
            // ipV4
            !addr.getHostAddress().contains(":"))
        .collect(Collectors.toList());
  }

  /**
   *
   * @return return the ip address of the device
   * @throws IOException in case of I/O errors
   */
  public static InetAddress getPrivateNetworkIpAddress() throws IOException {
    List<InetAddress> ipAddresses = getIpAddresses();
    if (ipAddresses.isEmpty()) {
      throw new IOException("No IP address were found");
    }
    if (ipAddresses.size() == 1) {
      return ipAddresses.get(0);
    }
    ipAddresses.sort(AddressUtils::compare);
    return ipAddresses.get(0);
  }

  private static int compare(InetAddress address1, InetAddress address2) {
    if (is16BlockPrivateAddress(address1)) {
      return -1;
    } else if (is16BlockPrivateAddress(address2)) {
      return 1;
    }
    if (is24BlockPrivateAddress(address1)) {
      return -1;
    } else if (is24BlockPrivateAddress(address2)) {
      return 1;
    }
    if (is20BlockPrivateAddress(address1)) {
      return -1;
    } else if (is20BlockPrivateAddress(address2)) {
      return 1;
    }
    return address1.getHostName().compareTo(address1.getHostName());
  }

  public static boolean is16BlockPrivateAddress(InetAddress address) {
    return isInRange(address, "192.168.0.0", "192.168.255.255");
  }

  public static boolean is24BlockPrivateAddress(InetAddress address) {
    return isInRange(address, "10.0.0.0", "10.255.255.255");
  }

  public static boolean is20BlockPrivateAddress(InetAddress address) {
    return isInRange(address, "172.16.0.0", "172.31.255.255");
  }

  @SneakyThrows
  public static boolean isInRange(InetAddress address, String lowestAddress,
      String highestAddress) {
    return isInRange(address, InetAddress.getByName(lowestAddress), InetAddress.getByName(highestAddress));
  }

  public static boolean isInRange(InetAddress address, InetAddress lowestAddress,
      InetAddress highestAddress) {
    long ipLo = ipToLong(lowestAddress);
    long ipHi = ipToLong(highestAddress);
    long ipToTest = ipToLong(address);
    return ipToTest >= ipLo && ipToTest <= ipHi;
  }

  public static long ipToLong(InetAddress ip) {
    byte[] octets = ip.getAddress();
    long result = 0;
    for (byte octet : octets) {
      result <<= 8;
      result |= octet & 0xff;
    }
    return result;
  }

  @SneakyThrows
  private static InetAddress getAddress(String address) {
    return InetAddress.getByName(address);
  }
}
