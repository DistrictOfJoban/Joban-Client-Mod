// Handwritten ABI 6/7 module: exercises loading and state without ABI 8 services.
#include <mtr/mtr_native.h>
#include <cstring>
extern "C" {
MTR_NATIVE_EXPORT uint32_t mtrNativeAbiVersion(){return LEGACY_ABI;}
MTR_NATIVE_EXPORT const char* mtrScriptType(){return "vehicle";}
MTR_NATIVE_EXPORT const char* mtrScriptId(){return "test:legacy";}
MTR_NATIVE_EXPORT size_t mtrStateSize(){return sizeof(int);}
MTR_NATIVE_EXPORT void mtrInit(const JcmFrameInput* in){*static_cast<int*>(in->state)=0;}
MTR_NATIVE_EXPORT int32_t mtrCreate(const JcmFrameInput* in){return in->abi_version==LEGACY_ABI?0:-1;}
MTR_NATIVE_EXPORT int32_t mtrRender(const JcmFrameInput* in,JcmFrameOutput* out){
    if(in->abi_version!=LEGACY_ABI||in->resource_kind!=0) return -1;
    (*static_cast<int*>(in->state))++;
    std::memset(out,0,sizeof(*out)); return 0;
}
MTR_NATIVE_EXPORT int32_t mtrDispose(const JcmFrameInput* in){return *static_cast<int*>(in->state)>0?0:-1;}
}
