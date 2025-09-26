package net.aqualoco.restrictiveclasses.role;

import java.util.List;
import java.util.Map;

    public record Role(
            String id,
            String display_name,
            String icon,
            Controls controls,
            Allow allow,
            Deny deny,
            Map<String, Float> effects
    ) {
        public record Controls(
                boolean break_block,
                boolean use_item,
                boolean use_block
        ) {
            public static Controls defaults() {
                return new Controls(true, true, true);
            }
        }

        public record Allow(
                List<String> block_tags,
                List<String> blocks,
                List<String> item_tags,
                List<String> items,
                List<String> use_block_tags,
                List<String> use_blocks
        ) {
            public static Allow empty() {
                return new Allow(List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
            }
        }

        public record Deny(
                List<String> block_tags,
                List<String> blocks,
                List<String> item_tags,
                List<String> items,
                List<String> use_block_tags,
                List<String> use_blocks
        ) {
            public static Deny empty() {
                return new Deny(List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
            }
        }

    }