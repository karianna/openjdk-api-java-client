/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.adoptopenjdk.v3.tests;

import net.adoptopenjdk.v3.api.AOV3Error;
import net.adoptopenjdk.v3.api.AOV3ExceptionParseFailed;
import net.adoptopenjdk.v3.api.AOV3Installer;
import net.adoptopenjdk.v3.vanilla.internal.AOV3ResponseParsers;
import net.adoptopenjdk.v3.vanilla.internal.AOV3ResponseParsersType;
import org.apache.commons.io.input.BrokenInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class AOV3ResponseParsersTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(AOV3ResponseParsersTest.class);

  private AOV3ResponseParsersType parsers;
  private ArrayList<AOV3Error> errors;

  @BeforeEach
  public void testSetup()
  {
    this.parsers = AOV3ResponseParsers.create();
    this.errors = new ArrayList<AOV3Error>();
  }

  private void logError(
    final AOV3Error error)
  {
    LOG.error("error: {}", error);
    this.errors.add(error);
  }

  @Test
  public void testAvailableReleases()
    throws Exception
  {
    final var stream = resource("availableReleases.json");
    final var parser = this.parsers.createParser(
      this::logError,
      URI.create("urn:test"),
      stream);
    final var releases = parser.parseAvailableReleases();

    Assertions.assertEquals(
      List.of(
        new BigInteger("8"),
        new BigInteger("11")
      ),
      releases.availableLTSReleases());

    Assertions.assertEquals(
      List.of(
        new BigInteger("8"),
        new BigInteger("9"),
        new BigInteger("10"),
        new BigInteger("11"),
        new BigInteger("12"),
        new BigInteger("13"),
        new BigInteger("14")
      ),
      releases.availableReleases());

    Assertions.assertEquals(
      new BigInteger("14"),
      releases.mostRecentFeatureRelease());

    Assertions.assertEquals(
      new BigInteger("11"),
      releases.mostRecentLTSRelease());
  }

  @Test
  public void testAvailableReleasesIOError()
  {
    final var stream = new BrokenInputStream();

    final var parser =
      this.parsers.createParser(
        this::logError,
        URI.create("urn:test"),
        stream);

    final var ex =
      Assertions.assertThrows(
        AOV3ExceptionParseFailed.class,
        parser::parseAvailableReleases
      );

    Assertions.assertEquals(IOException.class, ex.getCause().getClass());
  }

  @Test
  public void testReleaseNames()
    throws Exception
  {
    final var stream = resource("releaseNames.json");
    final var parser =
      this.parsers.createParser(this::logError, URI.create("urn:test"), stream);
    final var releases = parser.parseReleaseNames();
    Assertions.assertEquals(445, releases.size());
    Assertions.assertTrue(releases.contains("jdk-11.0.7+7_openj9-0.20.0-m1"));
    Assertions.assertTrue(releases.contains("jdk11u-2020-03-06-09-43"));

    Assertions.assertEquals(0, this.errors.size());
  }

  @Test
  public void testReleaseNamesIOError()
  {
    final var stream = new BrokenInputStream();

    final var parser =
      this.parsers.createParser(
        this::logError,
        URI.create("urn:test"),
        stream);

    final var ex =
      Assertions.assertThrows(
        AOV3ExceptionParseFailed.class,
        parser::parseReleaseNames
      );

    Assertions.assertEquals(IOException.class, ex.getCause().getClass());
  }

  @Test
  public void testReleaseVersions()
    throws Exception
  {
    final var stream = resource("releaseVersions.json");
    final var parser =
      this.parsers.createParser(this::logError, URI.create("urn:test"), stream);
    final var releases = parser.parseReleaseVersions();
    Assertions.assertEquals(0, this.errors.size());
  }

  @Test
  public void testReleaseVersionsIOError()
  {
    final var stream = new BrokenInputStream();

    final var parser =
      this.parsers.createParser(
        this::logError,
        URI.create("urn:test"),
        stream);

    final var ex =
      Assertions.assertThrows(
        AOV3ExceptionParseFailed.class,
        parser::parseReleaseVersions
      );

    Assertions.assertEquals(IOException.class, ex.getCause().getClass());
  }

  @Test
  public void testAssetsForRelease()
    throws Exception
  {
    final var stream = resource("assetsForRelease.json");
    final var parser =
      this.parsers.createParser(this::logError, URI.create("urn:test"), stream);
    final var assets = parser.parseAssetsForRelease();

    {
      final var release = assets.get(0);
      Assertions.assertEquals("MDc6UmVsZWFzZTIzMDczOTQ0", release.id());
    }

    {
      final var release = assets.get(9);
      Assertions.assertEquals("MDc6UmVsZWFzZTE5NTA2MjQ5", release.id());
    }

    Assertions.assertEquals(0, this.errors.size());
  }

  @Test
  public void testAssetsForReleaseIOError()
  {
    final var stream = new BrokenInputStream();

    final var parser =
      this.parsers.createParser(
        this::logError,
        URI.create("urn:test"),
        stream);

    final var ex =
      Assertions.assertThrows(
        AOV3ExceptionParseFailed.class,
        parser::parseAssetsForRelease
      );

    Assertions.assertEquals(IOException.class, ex.getCause().getClass());
  }

  @Test
  public void testAssetsForReleaseBad1()
    throws Exception
  {
    final var stream =
      resource("assetsForReleaseBad1.json");
    final var parser =
      this.parsers.createParser(this::logError, URI.create("urn:test"), stream);
    parser.parseAssetsForRelease();

    Assertions.assertEquals(1, this.errors.size());
  }

  @TestFactory
  public Stream<DynamicTest> testAssetsForReleaseFuzz()
  {
    return IntStream.range(1, 1000)
      .boxed()
      .map(index -> {
        return DynamicTest.dynamicTest(
          "testAssetsForReleaseFuzz" + index,
          () -> this.testAssetsForReleaseFuzzOnce(index));
      });
  }

  @Test
  public void testAssetsForLatest()
    throws Exception
  {
    final var stream = resource("assetsForLatest.json");
    final var parser =
      this.parsers.createParser(this::logError, URI.create("urn:test"), stream);
    final var assets = parser.parseAssetsForLatest();

    {
      final var release = assets.get(0);
      Assertions.assertEquals("jdk-11.0.6+10", release.releaseName());
    }

    {
      final var release = assets.get(25);
      Assertions.assertEquals("jdk-11.0.6+10", release.releaseName());
    }

    Assertions.assertEquals(0, this.errors.size());
  }

  @Test
  public void testAssetsForReleases8()
    throws Exception
  {
    final var stream = resource("releases8.json");
    final var parser =
      this.parsers.createParser(this::logError, URI.create("urn:test"), stream);
    final var releases = parser.parseAssetsForRelease();

    for (final var release : releases) {
      LOG.debug("release {}", release.id());
      for (final var binary : release.binaries()) {
        LOG.debug("release {} binary {}", release.id(), binary.package_().name());
        final var packChecksumOpt = binary.package_().checksum();
        if (packChecksumOpt.isPresent()) {
          final var checksum = packChecksumOpt.get();
          Assertions.assertFalse(checksum.isBlank());
        }

        final var instChecksumOpt =
          binary.installer().flatMap(AOV3Installer::checksum);
        if (instChecksumOpt.isPresent()) {
          final var checksum = instChecksumOpt.get();
          Assertions.assertFalse(checksum.isBlank());
        }
      }
    }

    Assertions.assertEquals(0, this.errors.size());
  }

  private void testAssetsForReleaseFuzzOnce(
    final Integer index)
    throws Exception
  {
    final var rng = new Random(index.longValue());
    try (var stream =
           AOV3Fuzzing.filterRemoveNodes(
             rng,
             resource("assetsForRelease.json"),
             0.001)) {
      final var parser =
        this.parsers.createParser(
          this::logError,
          URI.create("urn:test"),
          stream);
      parser.parseAssetsForRelease();
      Assertions.assertTrue(this.errors.size() > 0);
    }
  }

  private static InputStream resource(final String name)
    throws IOException
  {
    final var path =
      String.format("/net/adoptopenjdk/v3/tests/%s", name);
    final var url =
      AOV3ResponseParsersTest.class.getResource(path);
    if (url == null) {
      throw new FileNotFoundException(path);
    }
    return url.openStream();
  }
}
