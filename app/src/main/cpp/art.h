
#include <cwchar>

#ifndef FIXDALVIK_ART_H_H
#define FIXDALVIK_ART_H_H

#endif //FIXDALVIK_ART_H_H

namespace art {
    namespace mirror {
        class Object {
        public:
            //the number of vtable entries in java.lang.Object
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
            uint32_t super_class_;
            uint32_t clinit_thread_id_;

            uint32_t status_;

        };
        static void *java_lang_Class_;

//        class ArtField : public Object {
//            uint32_t declaring_class_;
//
//            uint32_t access_flags_;
//
//            // Dex cache index of field id
//            uint32_t field_dex_idx_;
//
//            // Offset of field within an instance or in the Class' static fields
//            uint32_t offset_;
//        };


        class ArtMethod : public Object {

        public:
            // Field order required by test "ValidateFieldOrderOfJavaCppUnionClasses".
            // The class we are a part of.
            uint32_t declaring_class_;
            // Access flags; low 16 bits are defined by spec.
            uint32_t access_flags_;

            /* Dex file fields. The defining dex file is available via declaring_class_->dex_cache_ */

            // Short cuts to declaring_class_->dex_cache_ member for fast compiled code access.
            uint32_t dex_cache_resolved_types_;

            uint32_t dex_cache_resolved_methods_;
            // Offset to the CodeItem.
            uint32_t dex_code_item_offset_;

            // Index into method_ids of the dex file associated with this method.
            uint32_t dex_method_index_;

            /* End of dex file fields. */

            // Entry within a dispatch table for this method. For static/direct methods the index is into
            // the declaringClass.directMethods, for virtual methods the vtable and for interface methods the
            // ifTable.
            uint32_t method_index_;

            // The hotness we measure for this method. Managed by the interpreter. Not atomic, as we allow
            // missing increments: if the method is hot, we will see it eventually.
            uint16_t hotness_count_;

            // Fake padding field gets inserted here.


            // Must be the last fields in the method.
            // PACKED(4) is necessary for the correctness of
            // RoundUp(OFFSETOF_MEMBER(ArtMethod, ptr_sized_fields_), pointer_size).

            struct PtrSizedFields {
                // Short cuts to declaring_class_->dex_cache_ member for fast compiled code access.
                ArtMethod **dex_cache_resolved_methods_;

//                // Short cuts to declaring_class_->dex_cache_ member for fast compiled code access.
//                GcRoot<mirror::Class>* dex_cache_resolved_types_;

                void *entry_point_from_interpreter_;
                // Pointer to JNI function registered to this method, or a function to resolve the JNI function,
                // or the profiling data for non-native methods, or an ImtConflictTable.
                void *entry_point_from_jni_;

                // Method dispatch from quick compiled code invokes this pointer which may cause bridging into
                // the interpreter.
                void *entry_point_from_quick_compiled_code_;
            } ptr_sized_fields_;

            static void *java_lang_reflect_ArtMethod_;

        };
    }
}
