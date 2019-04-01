/*
 * Hello Minecraft! Launcher.
 * Copyright (C) 2013  huangyuhui <huanghongxun2008@126.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see {http://www.gnu.org/licenses/}.
 */
package org.jackhuang.hellominecraft.launcher.core.version;

import org.jackhuang.hellominecraft.launcher.core.download.DownloadType;

/**
 *
 * @author huangyuhui
 */
public class GameDownloadInfo implements Cloneable {

    public String sha1;
    public int size;
    protected String url;

    /**
     * Ready for AssetIndexDownloadInfo, and GameDownloadInfo also need this.
     */
    public String id = null;

    /**
     * Get the game download url.
     *
     * @param dt where to download?
     *
     * @return the download url
     */
    public String getUrl(DownloadType dt) {
        return getUrl(dt, dt.getProvider().isAllowedToUseSelfURL());
    }

    /**
     * Get the game download url.
     *
     * @param dt        where to download?
     * @param allowSelf allow this game to be downloaded from its modified url?
     *
     * @return the download url
     */
    public String getUrl(DownloadType dt, boolean allowSelf) {
        if (url != null && allowSelf)
            return url;
        else
            return dt.getProvider().getVersionsDownloadURL() + id + "/" + id + ".jar";
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new Error(ex);
        }
    }
}
