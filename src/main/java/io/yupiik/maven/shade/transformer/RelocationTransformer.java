/*
 * Copyright (c) 2020 - Yupiik SAS - https://www.yupiik.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.yupiik.maven.shade.transformer;

import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;
import org.codehaus.plexus.util.IOUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarOutputStream;

/**
 * Trivial transformer applying relocators on resources content.
 */
public class RelocationTransformer implements ResourceTransformer {
    private RelocationHandler handler;
    private Collection<ResourceTransformer> delegates;
    private boolean transformed;

    public RelocationTransformer() {
        this(new ClassRelocationHandler());
    }

    protected RelocationTransformer(RelocationHandler handler) {
        this.handler = handler;
    }

    @Override
    public boolean canTransformResource(String resource) {
        if (delegates == null) {
            return false;
        }
        for (final ResourceTransformer transformer : delegates) {
            if (transformer.canTransformResource(resource)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void processResource(final String resource, final InputStream is, final List<Relocator> relocators) throws IOException {
        byte[] relocated = null;
        for (final ResourceTransformer transformer : delegates) {
            if (transformer.canTransformResource(resource)) {
                transformed = true;
                if (relocated == null) {
                    relocated = handler.relocate(IOUtil.toString(is), relocators)
                            .getBytes(StandardCharsets.UTF_8);
                }
                transformer.processResource(
                        resource,
                        new ByteArrayInputStream(relocated),
                        relocators);
            }
        }
    }

    @Override
    public boolean hasTransformedResource() {
        return transformed;
    }

    @Override
    public void modifyOutputStream(final JarOutputStream os) throws IOException {
        if (!transformed) {
            return;
        }
        for (final ResourceTransformer transformer : delegates) {
            if (transformer.hasTransformedResource()) {
                transformer.modifyOutputStream(os);
            }
        }
    }

    public void setDelegates(final Collection<ResourceTransformer> delegates) {
        this.delegates = delegates;
    }
}
