package de.pbma.nearfly;

import android.net.Uri;

class NearflyNice {
    public abstract class  NearflyMessage implements Comparable<NearflyMessage>{
        private String channel;
        private Integer nice;

        @Override
        public int compareTo(NearflyMessage other) {
            return this.getNice().compareTo(other.getNice());
        }

        public Integer getNice() {
            return nice;
        }

        public String getChannel() {
            return channel;
        }
    }

    public class NearflyTextMessage extends NearflyMessage{
        private String channel;
        private String payload;
        private Integer nice;

		public NearflyTextMessage(String channel, String payload, Integer nice) {
            this.channel = channel;
            this.payload = payload;
            this.nice = nice;
        }

        public Integer getNice() {
            return nice;
        }

        public String getChannel() {
            return channel;
        }

        public String getPayload() {
            return payload;
        }

        @Override
        public int compareTo(NearflyMessage other) {
            return nice.compareTo(other.getNice());
        }
    }

    public class NearflyFileMessage extends NearflyMessage{
        private Integer nice;
        private String channel;
        private Uri uri;
        private String textAttachment;

        public NearflyFileMessage(String channel, Uri uri, String textAttachment, Integer nice) {
            this.channel = channel;
            this.uri = uri;
            this.textAttachment = textAttachment;
            this.nice = nice;
        }

        public Integer getNice() {
            return nice;
        }

        public String getChannel() {
            return channel;
        }

        public Uri getUri() {
            return uri;
        }

        public String getTextAttachment() {
            return textAttachment;
        }

        @Override
        public int compareTo(NearflyMessage other) {
            return nice.compareTo(other.getNice());
        }
    }

    /*class Manager extends Thread{
        Manager

        @Override
        public void run() {
            super.run();

        }
    }*/
}
