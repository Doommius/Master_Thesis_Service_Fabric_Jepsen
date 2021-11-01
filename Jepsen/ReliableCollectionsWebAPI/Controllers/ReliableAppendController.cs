// ------------------------------------------------------------
//  Copyright (c) Microsoft Corporation.  All rights reserved.
//  Licensed under the MIT License (MIT). See License.txt in the repo root for license information.
// ------------------------------------------------------------

namespace ReliableCollectionsWebAPI.Controllers
{
    using System.Collections.Generic;
    using System.Threading;
    using System.Threading.Tasks;
    using Microsoft.AspNetCore.Mvc;
    using Microsoft.ServiceFabric.Data;
    using Microsoft.ServiceFabric.Data.Collections;
    using System.Net.Http;
    using System;
    using System.Fabric;
    using Newtonsoft.Json.Linq;

    [Route("api/[controller]")]
    public class ReliableAppendController : Controller
    {
        private readonly IReliableStateManager stateManager;

        public ReliableAppendController(IReliableStateManager stateManager)
        {
            this.stateManager = stateManager;
        }

        public System.Fabric.ReplicaRole ReplicaRole { get; }


        [HttpGet]
        public async Task<IActionResult> Get()
        {
            CancellationToken ct = new CancellationToken();

            IReliableDictionary<string, List<long>> votesDictionary = await this.stateManager.GetOrAddAsync<IReliableDictionary<string, List<long>>>("appenworkload");

            using (ITransaction tx = this.stateManager.CreateTransaction())
            {
                var list = await votesDictionary.CreateEnumerableAsync(tx);

                var enumerator = list.GetAsyncEnumerator();

                List<KeyValuePair<string, List<long>>> result = new List<KeyValuePair<string, List<long>>>();

                while (await enumerator.MoveNextAsync(ct))
                {
                    result.Add(enumerator.Current);
                }

                return this.Json(result);
            }
        }

        // GET VoteData/name
        [HttpGet("{key}")]
        public async Task<IActionResult> Get(string key)
        {


            IReliableDictionary<string, List<long>> votesDictionary = await this.stateManager.GetOrAddAsync<IReliableDictionary<string, List<long>>>("appenworkload");
            List<KeyValuePair<string, List<long>>> result = new List<KeyValuePair<string, List<long>>>();


            using (ITransaction tx = this.stateManager.CreateTransaction())
            {
                ConditionalValue<List<long>> conditionalValue = await votesDictionary.TryGetValueAsync(tx, key);
                if (conditionalValue.HasValue)
                {
                    List<long> value = conditionalValue.Value;
                    result.Add(new KeyValuePair<string, List<long>>(key, value));

                    return this.Json(result);
                }
                else
                {
                    return NoContent();
                }

            }
        }

        [HttpPut]
        public async Task<IActionResult> Put()
        {
            List<KeyValuePair<string, List<long>>> result = new List<KeyValuePair<string, List<long>>>();
            try

            {
                IReliableDictionary<string, List<long>> votesDictionary = await this.stateManager.GetOrAddAsync<IReliableDictionary<string, List<long>>>("appenworkload");

                String transactionquery;
                if (!String.IsNullOrEmpty(HttpContext.Request.Query["query"]))
                {
                    transactionquery = HttpContext.Request.Query["query"];
                }
                else
                {
                    return NoContent();
                }
                
                dynamic operationlist;

                operationlist = Newtonsoft.Json.JsonConvert.DeserializeObject(transactionquery);

                using (ITransaction tx = this.stateManager.CreateTransaction())
                {
                    ConditionalValue<List<long>> conditionalValue;
                    Boolean v;
                    foreach (var item in operationlist.transaction)
                    {

                        if (item.operation.Value == "r")
                        {
                            conditionalValue = await votesDictionary.TryGetValueAsync(tx, item.key.Value);
                            if (conditionalValue.HasValue)
                            {
                                List<long> value = conditionalValue.Value;
                                result.Add(new KeyValuePair<string, List<long>>(item.key.Value, new List<long>(value)));

                            }
                            else
                            {
                                result.Add(new KeyValuePair<string, List<long>>(item.key.Value, null ));
                            }
                        }

                        else if (item.operation.Value == "w")
                        {

                            result.Add(new KeyValuePair<string, List<long>>(item.key.Value, new List<long>(await votesDictionary.SetAsync(tx, item.key.Value, item.value.ToObject(typeof(List<long>))))));
                        }
                        else if (item.operation.Value == "a")
                        {
                            //(tx, key, value, (key, oldvalue) => value);
                            long tmpvalue = item.value.Value;
                            string key = item.key.Value;

                            result.Add(new KeyValuePair<string, List<long>>(item.key.Value, new List<long>(await votesDictionary.AddOrUpdateAsync(tx, key, new List<long>() { tmpvalue }, (key, oldvalue) => { oldvalue.Add(tmpvalue); return oldvalue; }))));




                        }
                        else if (item.operation.Value == "c")
                        {
                            v = await votesDictionary.TryUpdateAsync(tx, item.key.Value, item.value.ToObject(typeof(List<long>)), item.expected.ToObject(typeof(List<long>)));
                            if (!v)
                            {
                                result.Add(new KeyValuePair<string, List<long>>(item.key.Value, new List<long>() { -1 }));
                            }
                            else
                            {
                                result.Add(new KeyValuePair<string, List<long>>(item.key.Value, new List<long>() { -2 }));
                            }

                        }
                        else if (item.operation.Value == "abort")
                        {

                            tx.Abort();
                            result.Add(new KeyValuePair<string, List<long>>(item.operation.Value, new List<long>() { -10 }));
                            return this.Json(result);
                        }
                        else if (item.operation.Value == "d")
                        {
                            conditionalValue = await votesDictionary.TryRemoveAsync(tx, item.key.Value);
                            result.Add(new KeyValuePair<string, List<long>>(item.key.Value, conditionalValue.Value));
                        }
                        else
                        {
                            result.Add(new KeyValuePair<string, List<long>>(item.operation.Value, new List<long>() { -1 }));
                        }
                    }
                 await tx.CommitAsync();
                }
                return this.Json(result);
            }
            catch (Exception e)
            {
                result.Add(new KeyValuePair<string, List<long>>(e.ToString(), new List<long>() { -1 }));
                return this.Json(result);
            }
        }



        [HttpPut("{key}/{value}")]
        public async Task<IActionResult> Put(string key, long value)
        {
            IReliableDictionary<string, List<long>> votesDictionary = await this.stateManager.GetOrAddAsync<IReliableDictionary<string, List<long>>>("appenworkload");
            try
            {
                using (ITransaction tx = this.stateManager.CreateTransaction())
                {
                    await votesDictionary.AddOrUpdateAsync(tx, key, new List<long>() { value }, (key, oldvalue) => new List<long>() { value });
                    await tx.CommitAsync();
                }

                List<KeyValuePair<string, long>> result = new List<KeyValuePair<string, long>>();
                result.Add(new KeyValuePair<string, long>(key, value));
                return this.Json(result);
            }
            catch (FabricNotPrimaryException)
            {
                return new ForbidResult("Not Primary");
            }


        }


        // POST VoteData/cas
        [HttpPut("{key}/{value}/{expected}")]
        public async Task<IActionResult> put(string key, long value, long expected)
        {
            IReliableDictionary<string, List<long>> votesDictionary = await this.stateManager.GetOrAddAsync<IReliableDictionary<string, List<long>>>("appenworkload");
            List<KeyValuePair<string, List<long>>> result = new List<KeyValuePair<string, List<long>>>();

            List<long> returnvalue = new List<long>() { value };
            Boolean v;
            try
            {
                using (ITransaction tx = this.stateManager.CreateTransaction())
                {
                    v = await votesDictionary.TryUpdateAsync(tx, key, new List<long>() { value }, new List<long>() { expected });
                    await tx.CommitAsync();
                }
            }
            catch (FabricNotPrimaryException)
            {
                return new ForbidResult("Not Primary");
            }
 
            if (!v)
            {
                using (ITransaction tx = this.stateManager.CreateTransaction())
                {
                    ConditionalValue<List<long>> conditionalValue = await votesDictionary.TryGetValueAsync(tx, key);
                    returnvalue = conditionalValue.Value;
                }

            }

            result.Add(new KeyValuePair<string, List<long>>(key, returnvalue));
            return this.Json(result);

        }


        // DELETE VoteData/name
        [HttpDelete("{key}")]
        public async Task<IActionResult> Delete(string key)
        {
            IReliableDictionary<string, List<long>> votesDictionary = await this.stateManager.GetOrAddAsync<IReliableDictionary<string, List<long>>>("appenworkload");
            try
            {
                using (ITransaction tx = this.stateManager.CreateTransaction())
                {
                    if (await votesDictionary.ContainsKeyAsync(tx, key))
                    {
                        await votesDictionary.TryRemoveAsync(tx, key);
                        await tx.CommitAsync();
                        return new OkResult();
                    }
                    else
                    {
                        return new NotFoundResult();
                    }
                }
            }
            catch (FabricNotPrimaryException)
            {
                return new ForbidResult("Not Primary");
            }

        }
    }
}