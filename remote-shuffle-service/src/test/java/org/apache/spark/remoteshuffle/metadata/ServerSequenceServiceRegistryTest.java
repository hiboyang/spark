package org.apache.spark.remoteshuffle.metadata;

import org.apache.spark.remoteshuffle.common.ServerDetail;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ServerSequenceServiceRegistryTest {
  @Test
  public void oneServerSequence() {
    ServerSequenceServiceRegistry serviceRegistry =
        new ServerSequenceServiceRegistry("server-%s", "server-%s.domain", 0, 0);

    Assert.assertEquals(
        serviceRegistry.getServers("dc1", "cluster1", 0, Collections.emptyList()).size(), 0);

    List<ServerDetail> result =
        serviceRegistry.getServers("dc1", "cluster1", Integer.MAX_VALUE, Collections.emptyList());
    Assert.assertEquals(result.size(), 1);
    Assert.assertEquals(result.get(0).getServerId(), "server-0");
    Assert.assertEquals(result.get(0).getConnectionString(), "server-0.domain");

    result = serviceRegistry.lookupServers("dc1", "cluster1", Arrays.asList("server-0"));
    Assert.assertEquals(result.size(), 1);
    Assert.assertEquals(result.get(0).getServerId(), "server-0");
    Assert.assertEquals(result.get(0).getConnectionString(), "server-0.domain");
  }

  @Test
  public void multiServerSequence() {
    ServerSequenceServiceRegistry serviceRegistry =
        new ServerSequenceServiceRegistry("server-%s", "server-%s.domain", 0, 2);

    Assert.assertEquals(
        serviceRegistry.getServers("dc1", "cluster1", 0, Collections.emptyList()).size(), 0);

    List<ServerDetail> result =
        serviceRegistry.getServers("dc1", "cluster1", Integer.MAX_VALUE, Collections.emptyList());
    result.sort(Comparator.comparing(ServerDetail::getServerId));
    Assert.assertEquals(result.size(), 3);
    Assert.assertEquals(result.get(0).getServerId(), "server-0");
    Assert.assertEquals(result.get(0).getConnectionString(), "server-0.domain");
    Assert.assertEquals(result.get(2).getServerId(), "server-2");
    Assert.assertEquals(result.get(2).getConnectionString(), "server-2.domain");

    result =
        serviceRegistry.lookupServers("dc1", "cluster1", Arrays.asList("server-0", "server-2"));
    result.sort(Comparator.comparing(ServerDetail::getServerId));
    Assert.assertEquals(result.size(), 2);
    Assert.assertEquals(result.get(0).getServerId(), "server-0");
    Assert.assertEquals(result.get(0).getConnectionString(), "server-0.domain");
    Assert.assertEquals(result.get(1).getServerId(), "server-2");
    Assert.assertEquals(result.get(1).getConnectionString(), "server-2.domain");
  }
}
