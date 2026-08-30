package org.the3deer.util.bean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

public class BeanManagerTest {

    public static class TestCamera {
        public String name;
        public TestCamera() {}
        public TestCamera(String name) { this.name = name; }
    }

    public static class RootConsumer {
        @Inject
        public TestCamera camera;

        @Inject
        @Named("gui\\.camera-")
        public TestCamera privateCamera;

        @Inject
        @Named(".*")
        public List<TestCamera> allCameras;
    }

    public static class NamespacedConsumer {
        @Inject
        public TestCamera camera;
    }

    @Test
    public void testLookupPriorityAndExplicitPrivacy() {
        BeanManager beanManager = BeanManager.getInstance();

        // 1. Root camera (public)
        TestCamera rootCamera = new TestCamera("root");
        beanManager.add("camera", rootCamera);

        // 2. GUI camera (private)
        TestCamera guiCamera = new TestCamera("gui");
        beanManager.add("gui.camera-", guiCamera);

        // 3. Namespaced camera (public)
        TestCamera otherCamera = new TestCamera("other");
        beanManager.add("other.camera", otherCamera);

        // 4. Root consumer
        RootConsumer rootConsumer = new RootConsumer();
        beanManager.add("consumer", rootConsumer);

        // 5. GUI consumer
        NamespacedConsumer guiConsumer = new NamespacedConsumer();
        beanManager.add("gui.consumer", guiConsumer);

        // 6. Other consumer
        NamespacedConsumer otherConsumer = new NamespacedConsumer();
        beanManager.add("other.consumer", otherConsumer);

        beanManager.initialize();
        beanManager.start();

        // Root consumer:
        // - 'camera' should find root camera (Phase 1: hierarchy)
        assertEquals("root", rootConsumer.camera.name);
        // - 'privateCamera' with @Named should find the private gui camera
        assertNotNull(rootConsumer.privateCamera);
        assertEquals("gui", rootConsumer.privateCamera.name);
        // - 'allCameras' with @Named(".*") should find ALL 3
        assertEquals(3, rootConsumer.allCameras.size());

        // GUI consumer (in 'gui' namespace):
        // - Phase 1 (hierarchy) should find 'gui.camera-' immediately.
        assertEquals("gui", guiConsumer.camera.name);

        // Other consumer (in 'other' namespace):
        // - Phase 1 (hierarchy) should find 'other.camera'.
        assertEquals("other", otherConsumer.camera.name);
    }

    @Test
    public void testSiblingVisibility() {
        BeanManager beanManager = BeanManager.getInstance();

        // Public bean in 'a' namespace
        TestCamera camA = new TestCamera("a");
        beanManager.add("a.camera", camA);

        // Consumer in 'b' namespace
        NamespacedConsumer consumerB = new NamespacedConsumer();
        beanManager.add("b.consumer", consumerB);

        beanManager.initialize();
        beanManager.start();

        // Sibling 'b' should see public bean in 'a' (Phase 2: Global Search)
        assertNotNull(consumerB.camera);
        assertEquals("a", consumerB.camera.name);
    }

    @Test
    public void testExplicitPrivacyBlocksSiblings() {
        BeanManager beanManager = BeanManager.getInstance();

        // Private bean in 'a' namespace
        TestCamera camA = new TestCamera("a-");
        beanManager.add("a.camera-", camA);

        // Consumer in 'b' namespace
        NamespacedConsumer consumerB = new NamespacedConsumer();
        beanManager.add("b.consumer", consumerB);

        beanManager.initialize();
        beanManager.start();

        // Sibling 'b' should NOT see private bean in 'a'
        assertNull(consumerB.camera);
    }

    @Test
    public void testChildNamespaceVisibilityForPublicBeans() {
        BeanManager beanManager = BeanManager.getInstance();

        // Bean in a child namespace
        TestCamera childCam = new TestCamera("child");
        beanManager.add("shader.v2.skybox", childCam);

        // Consumer in the parent namespace
        class ShaderManager {
            @Inject
            public java.util.Map<String, TestCamera> programs;
        }
        ShaderManager manager = new ShaderManager();
        beanManager.add("shader.manager", manager);

        beanManager.initialize();
        beanManager.start();

        // ShaderManager should see the skybox program because it's public
        assertNotNull(manager.programs);
        assertEquals(1, manager.programs.size());
        assertEquals("child", manager.programs.get("shader.v2.skybox").name);
    }
}
