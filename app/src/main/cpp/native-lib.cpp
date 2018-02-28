#include <jni.h>
#include "art.h"
#include <string>

extern "C"
JNIEXPORT void JNICALL
Java_andfix_cn_lee_fixdalvik_DxManager_replace(JNIEnv *env, jobject instance, jobject wrongMethod,
                                               jobject rightMethod) {
    //拿到错误class 字节码里面的方法表里的ArtMethod
    art::mirror::ArtMethod *smeth = (art::mirror::ArtMethod *) env->FromReflectedMethod(
            wrongMethod);
    //拿到正确class 字节码里面的方法表里的ArtMethod
    art::mirror::ArtMethod *dmeth = (art::mirror::ArtMethod *) env->FromReflectedMethod(
            rightMethod);

    //替换artMethod结构体的所有成员变量的指针
    smeth->declaring_class_= dmeth->declaring_class_;
    smeth->dex_cache_resolved_types_ = dmeth->dex_cache_resolved_types_;
    smeth->access_flags_ = dmeth->access_flags_ | 0x0001;
    smeth->dex_cache_resolved_methods_ = dmeth->dex_cache_resolved_methods_;
    smeth->dex_code_item_offset_ = dmeth->dex_code_item_offset_;
    smeth->method_index_ = dmeth->method_index_;
    smeth->dex_method_index_ = dmeth->dex_method_index_;
    smeth->method_dex_index_ = dmeth->method_dex_index_;
//    smeth->hotness_count_ = dmeth->hotness_count_;

    smeth->ptr_sized_fields_.dex_cache_resolved_methods_ = dmeth->ptr_sized_fields_.dex_cache_resolved_methods_;
    smeth->ptr_sized_fields_.entry_point_from_interpreter_ = dmeth->ptr_sized_fields_.entry_point_from_interpreter_;
    smeth->ptr_sized_fields_.entry_point_from_jni_ = dmeth->ptr_sized_fields_.entry_point_from_jni_;
    smeth->ptr_sized_fields_.entry_point_from_quick_compiled_code_ = dmeth->ptr_sized_fields_.entry_point_from_quick_compiled_code_;

}


