package io.nessus.test.common;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.common.Parameters;

public class ParametersTest {

    @Test
    public void testParser() {

        Parameters parA = new Parameters();
        parA.put("aaa", 50);
        parA.put("bbb", 5);
        parA.put("ccc", 10);

        String digest = "{aaa=50, bbb=5, ccc=10}";
        Assert.assertEquals(digest, parA.toString());

        Parameters parB = Parameters.fromString(digest);
        Assert.assertEquals(parA, parB);
    }

    @Test
    public void testImmutable() {

        Parameters parA = new Parameters();
        parA.put("aaa", 50);
        parA.put("bbb", 5);

        String digestA = "{aaa=50, bbb=5}";
        Assert.assertEquals(digestA, parA.toString());
        Assert.assertFalse(parA.isImmutable());
        
        Parameters parB = Parameters.fromString(digestA);
        parB.put("ccc", 10);
        String digestB = "{aaa=50, bbb=5, ccc=10}";
        Assert.assertEquals(digestB, parB.toString());
        
        Parameters parC = parB.makeImmutable();
        Assert.assertTrue(parC.isImmutable());
        
        try {
            parC.remove("ccc");
            Assert.fail("RuntimeException expected");
        } catch (RuntimeException rte) {
            // expected
        }
        
        Parameters parD = new Parameters(parC);
        Assert.assertFalse(parD.isImmutable());
        parD.remove("ccc");
        Assert.assertEquals(digestA, parD.toString());
        Assert.assertEquals(parA, parD);
    }

    @Test
    public void testStringArr() {

        List<String> lstA = Arrays.asList("aaa", "bbb", "ccc");
        Parameters parA = new Parameters();
        parA.put("aux", lstA);
        
        String digest = "{aux=[aaa, bbb, ccc]}";
        Assert.assertEquals(digest, parA.toString());
        
        Parameters parB = Parameters.fromString(digest);
        Assert.assertEquals(parA, parB);
        
        @SuppressWarnings("unchecked")
        List<String> lstB = parB.get("aux", List.class);
        Assert.assertEquals(lstA, lstB);
    }
}
