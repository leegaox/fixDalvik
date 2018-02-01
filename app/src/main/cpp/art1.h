
#include <cwchar>

#ifndef FIXDALVIK_ART_H_H
#define FIXDALVIK_ART_H_H

#endif //FIXDALVIK_ART_H_H

namespace art {
    namespace mirror {
        class Object {
        public:
            static const size_t kVTableLength = 11;
            static uint32_t hash_code_seed;
            uint32_t klass_;

            uint32_t monitor_;
        };

        class Class : public Object {
        public:
            static const size_t kImtSize = 64;
            uint32_t class_loader_;

            uint32_t component_type_;

            uint32_t dex_cache_;

            uint32_t dex_cache_strings_;

            uint32_t direct_methods_;

            uint32_t fields_;

        };

        class ArtField : public Object {
        public:
            uint32_t declaring_class_;
            uint32_t access_flags_;
            uint32_t field_dex_idx_;
            uint32_t offset_;
        };

        class ArtMethod : public Object {

            uint32_t declaring_class_;

            uint32_t dex_cache_resolved_methods_;

            uint32_t dex_cache_resolved_types_;

            uint32_t access_flags_;

            uint32_t dex_code_item_offset_;

            uint32_t dex_method_index_;

            uint32_t method_index_;

            struct PtrSizedFields {
                void *entry_point_from_interpreter_;

                void *entry_point_from_jni_;

                void *entry_point_from_quick_compiled_code_;
            };

            static void *java_lang_reflect_ArtMethod_;

        };


    }
}
