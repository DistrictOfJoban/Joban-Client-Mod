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
    std::memset(out,0,sizeof(*out));
    /* Deliberately malformed outputs for the bridge's descriptor guard.
       Normal legacy fixtures carry a zero snapshot and stay empty. */
    int32_t probe = 0;
    if(in->snapshot) std::memcpy(&probe,in->snapshot,sizeof(probe));
    if(probe==0x4A434D01) { out->record_count=1; out->records_len=8; }
    if(probe==0x4A434D02) {
        alignas(8) static uint8_t records[16]{};
        auto* header=reinterpret_cast<JcmRecordHeader*>(records);
        header->record_size=sizeof(records);
        out->record_count=1; out->records=records;
        out->records_len=sizeof(records); out->records_capacity=8;
    }
    return 0;
}
MTR_NATIVE_EXPORT int32_t mtrDispose(const JcmFrameInput* in){return *static_cast<int*>(in->state)>0?0:-1;}
}
